package github.yukinomiu.directsocks.client.server;

import github.yukinomiu.directsocks.client.exception.ClientRuntimeException;
import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.rfc.SocksAuthMethod;
import github.yukinomiu.directsocks.common.rfc.SocksRequest;
import github.yukinomiu.directsocks.common.rfc.SocksResponse;
import github.yukinomiu.directsocks.common.rfc.SocksVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ClientNioHandle.class);

    private final ClientConfig clientConfig;

    public ClientNioHandle(final ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public void handleRead(CubeContext cubeContext) {
        ClientContext clientContext = (ClientContext) cubeContext.attachment();
        if (clientContext == null) {
            clientContext = new ClientContext();
            clientContext.setClientProxyState(ClientProxyState.SOCKS_AUTH);

            cubeContext.attach(clientContext);
        }

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = cubeContext.getWriteBuffer();

        ClientProxyState currentState = clientContext.getClientProxyState();
        switch (currentState) {
            case SOCKS_AUTH:
                handleSocksAuth(cubeContext, clientContext, readBuffer, writeBuffer);
                break;

            case SOCKS_PROXY:
                handleSocksProxy(cubeContext, clientContext, readBuffer, writeBuffer);
                break;

            case DIRECT_SOCKS:
                handleDirectSocks(cubeContext, clientContext, readBuffer, writeBuffer);
                break;

            default:
                throw new ClientRuntimeException(currentState.name() + " 状态不支持");
        }
    }

    private void handleSocksAuth(final CubeContext cubeContext,
                                 final ClientContext clientContext,
                                 final ByteBuffer readBuffer,
                                 final ByteBuffer writeBuffer) {

        if (readBuffer.remaining() < 3) {
            logger.warn("协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS版本 {} 不支持", readBuffer.get(0));
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != readBuffer.remaining()) {
            logger.warn("协议错误");
            cubeContext.cancel();
            return;
        }

        boolean authSupport = false;
        while (readBuffer.hasRemaining()) {
            if (readBuffer.get() == SocksAuthMethod.NO_AUTH) {
                authSupport = true;
                break;
            }
        }

        clientContext.setClientProxyState(ClientProxyState.SOCKS_PROXY);

        writeBuffer.put(SocksVersion.VERSION_5);
        if (!authSupport) {
            logger.warn("SOCKS验证协议不支持");
            writeBuffer.put(SocksAuthMethod.NO_ACCEPTABLE_METHODS);
            clientContext.setSocksAuthMethod(SocksAuthMethod.NO_ACCEPTABLE_METHODS);

            cubeContext.cancelAfterWrite();
        } else {
            writeBuffer.put(SocksAuthMethod.NO_AUTH);
            clientContext.setSocksAuthMethod(SocksAuthMethod.NO_AUTH);
        }
    }

    private void handleSocksProxy(final CubeContext cubeContext,
                                  final ClientContext clientContext,
                                  final ByteBuffer readBuffer,
                                  final ByteBuffer writeBuffer) {

        if (readBuffer.remaining() < 4) {
            logger.warn("协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS版本 {} 不支持", readBuffer.get(0));
            cubeContext.cancel();
            return;
        }

        byte command = readBuffer.get();

        if (command != SocksRequest.COMMAND_CONNECT
                && command != SocksRequest.COMMAND_BIND
                && command != SocksRequest.COMMAND_UDP_ASSOCIATE) {

            logger.warn("协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksRequest.RESERVED) {
            logger.warn("协议错误");
            cubeContext.cancel();
            return;
        }

        byte addressType = readBuffer.get();
        byte[] address;
        byte[] port;

        if (addressType == SocksRequest.ATYP_IPV4) {
            if (readBuffer.remaining() != 6) {
                logger.warn("协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[4];
            port = new byte[2];
            readBuffer.get(address, 0, 4);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksRequest.ATYP_DOMAIN_NAME) {
            if (readBuffer.remaining() < 1) {
                logger.warn("协议错误");
                cubeContext.cancel();
                return;
            }

            int domainNameLength = readBuffer.get();

            if (readBuffer.remaining() != domainNameLength + 2) {
                logger.warn("协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[domainNameLength];
            port = new byte[2];
            readBuffer.get(address, 0, domainNameLength);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksRequest.ATYP_IPV6) {
            if (readBuffer.remaining() != 18) {
                logger.warn("协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[16];
            port = new byte[2];
            readBuffer.get(address, 0, 16);
            readBuffer.get(port, 0, 2);

        } else {
            logger.warn("地址类型 {} 不支持", addressType);
            cubeContext.cancel();
            return;
        }

        clientContext.setClientProxyState(ClientProxyState.DIRECT_SOCKS);
        clientContext.setCommand(command);
        clientContext.setAddressType(addressType);
        clientContext.setAddress(address);
        clientContext.setPort(port);

        InetAddress localBindAddress = clientConfig.getBindAddress();
        short portShort = (short) clientConfig.getBindPort().intValue();

        byte boundAddressType;
        byte[] boundAddress;

        if (localBindAddress instanceof Inet4Address) {
            boundAddressType = SocksResponse.ATYP_IPV4;
            boundAddress = localBindAddress.getAddress();
        } else if (localBindAddress instanceof Inet6Address) {
            boundAddressType = SocksResponse.ATYP_IPV6;
            boundAddress = localBindAddress.getAddress();
        } else {
            throw new ClientRuntimeException("本地监听地址类型不支持");
        }

        writeBuffer.put(SocksVersion.VERSION_5);

        if (command != SocksRequest.COMMAND_CONNECT) {
            writeBuffer.put(SocksResponse.REP_COMMAND_NOT_SUPPORTED);
            cubeContext.cancel();
        } else {
            writeBuffer.put(SocksResponse.REP_SUCCEEDED);
        }

        writeBuffer.put(SocksResponse.RESERVED);
        writeBuffer.put(boundAddressType);
        writeBuffer.put(boundAddress);
        writeBuffer.putShort(portShort);
    }

    private void handleDirectSocks(final CubeContext cubeContext,
                                   final ClientContext clientContext,
                                   final ByteBuffer readBuffer,
                                   final ByteBuffer writeBuffer) {

        byte[] b = new byte[readBuffer.remaining()];
        readBuffer.get(b, 0, b.length);
        logger.info(">>> data: {}", new String(b, StandardCharsets.US_ASCII));
        logger.info(">>> context: {}", clientContext.toString());
    }
}
