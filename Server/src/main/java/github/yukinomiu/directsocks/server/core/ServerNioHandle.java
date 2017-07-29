package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.auth.TokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
import github.yukinomiu.directsocks.common.protocol.DirectSocksAddressType;
import github.yukinomiu.directsocks.common.protocol.DirectSocksConnectionType;
import github.yukinomiu.directsocks.common.protocol.DirectSocksReply;
import github.yukinomiu.directsocks.common.protocol.DirectSocksVersion;
import github.yukinomiu.directsocks.server.exception.ServerInitException;
import github.yukinomiu.directsocks.server.exception.ServerRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ServerNioHandle.class);

    private final ServerConfig serverConfig;

    public ServerNioHandle(final ServerConfig serverConfig) throws ServerInitException {
        checkConfig(serverConfig);
        this.serverConfig = serverConfig;
    }

    @Override
    public void handleRead(CubeContext cubeContext) {
        ServerContext serverContext = (ServerContext) cubeContext.attachment();
        if (serverContext == null) {
            serverContext = new ServerContext(ServerChannelRole.CLIENT_ROLE);
            ClientRoleContext clientRoleContext = (ClientRoleContext) serverContext.getCurrentChannelContext();
            clientRoleContext.setServerState(ServerState.DIRECT_SOCKS_AUTH);

            cubeContext.attach(serverContext);
        }

        final ServerChannelRole currentRole = serverContext.getServerChannelRole();
        switch (currentRole) {
            case CLIENT_ROLE:
                handleClientRead(cubeContext, serverContext);
                break;

            case TARGET_ROLE:
                handleTargetRead(cubeContext, serverContext);
                break;

            default:
                throw new ServerRuntimeException(currentRole.name() + "角色不支持");
        }
    }

    private void handleClientRead(final CubeContext cubeContext, final ServerContext serverContext) {
        final ClientRoleContext clientRoleContext = (ClientRoleContext) serverContext.getCurrentChannelContext();
        ServerState currentState = clientRoleContext.getServerState();
        switch (currentState) {
            case DIRECT_SOCKS_AUTH:
                handleClientDirectSocksAuthRead(cubeContext, serverContext, clientRoleContext);
                break;

            case DIRECT_SOCKS:
                handleClientDirectSocksRead(cubeContext, serverContext, clientRoleContext);
                break;

            default:
                throw new ServerRuntimeException(currentState.name() + " 状态不支持");

        }
    }

    private void handleTargetRead(final CubeContext cubeContext, final ServerContext serverContext) {
        CubeContext clientCubeContext = serverContext.getAssociatedCubeContext();

        // exchange
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        ByteBuffer writeBuffer = clientCubeContext.readyWrite();
        writeBuffer.put(readBuffer);
    }

    private void handleClientDirectSocksAuthRead(final CubeContext cubeContext,
                                                 final ServerContext serverContext,
                                                 final ClientRoleContext clientRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final int authLength = serverConfig.getTokenConverter().targetLength();

        if (readBuffer.remaining() < authLength + 3) {
            logger.warn("DS协议错误·");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != DirectSocksVersion.VERSION_1) {
            logger.warn("DS版本 {} 不支持, 断开连接", readBuffer.get(0));
            cubeContext.cancel();
            return;
        }

        final byte[] token = new byte[authLength];
        readBuffer.get(token, 0, authLength);

        final byte connectionType = readBuffer.get();
        if (connectionType != DirectSocksConnectionType.TCP && connectionType != DirectSocksConnectionType.UDP) {
            logger.warn("DS协议错误·");
            cubeContext.cancel();
            return;
        }

        final byte addressType = readBuffer.get();
        final byte[] address;
        final short portShort;

        if (addressType == DirectSocksAddressType.IPV4) {
            if (readBuffer.remaining() != 6) {
                logger.warn("DS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[4];
            readBuffer.get(address, 0, 4);
            portShort = readBuffer.getShort();

        } else if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
            if (readBuffer.remaining() < 1) {
                logger.warn("DS协议错误");
                cubeContext.cancel();
                return;
            }

            int domainNameLength = readBuffer.get();

            if (readBuffer.remaining() != domainNameLength + 2) {
                logger.warn("DS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[domainNameLength];
            readBuffer.get(address, 0, domainNameLength);
            portShort = readBuffer.getShort();

        } else if (addressType == DirectSocksAddressType.IPV6) {
            if (readBuffer.remaining() != 18) {
                logger.warn("DS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[16];
            readBuffer.get(address, 0, 16);
            portShort = readBuffer.getShort();

        } else {
            logger.warn("DS协议错误");
            cubeContext.cancel();
            return;
        }

        // set context
        clientRoleContext.setToken(token);
        clientRoleContext.setConnectionType(connectionType);
        clientRoleContext.setAddressType(addressType);
        clientRoleContext.setAddress(address);
        clientRoleContext.setPort(portShort);

        // check context
        boolean authSuccess = serverConfig.getTokenChecker().check(token);
        if (!authSuccess) {
            String remoteIPString = cubeContext.getRemoteAddress().getHostAddress();
            String remotePortString = String.valueOf(cubeContext.getRemotePort());
            logger.info("验证失败, 断开连接, 远程地址 {}:{}", remoteIPString, remotePortString);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.AUTH_FAIL);
            cubeContext.cancelAfterWrite();
            return;
        }

        if (connectionType != DirectSocksConnectionType.TCP) {
            logger.info("连接类型 {} 不支持, 断开连接", connectionType);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.CONNECTION_TYPE_NOT_SUPPORTED);
            cubeContext.cancelAfterWrite();
            return;
        }

        // connect server
        final InetAddress targetAddress;
        try {
            if (addressType == DirectSocksAddressType.IPV4) {
                targetAddress = InetAddress.getByAddress(address);

            } else if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
                String targetHost = new String(address, StandardCharsets.US_ASCII);
                targetAddress = InetAddress.getByName(targetHost);

            } else if (addressType == DirectSocksAddressType.IPV6) {
                targetAddress = InetAddress.getByAddress(address);

            } else {
                throw new ServerRuntimeException("地址类型不支持");
            }
        } catch (UnknownHostException e) {
            logger.info("解析目标地址异常", e);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
            cubeContext.cancelAfterWrite();
            return;
        }

        try {
            SocketAddress targetSocketAddress = new InetSocketAddress(targetAddress, (int) portShort);

            CubeContext targetCubeContext = cubeContext.connectAndRegisterNewSocketChannel(targetSocketAddress);

            ServerContext targetServerContext = new ServerContext(ServerChannelRole.TARGET_ROLE);
            targetCubeContext.attach(targetServerContext);

            // associate
            serverContext.setAssociatedCubeContext(targetCubeContext);
            targetServerContext.setAssociatedCubeContext(cubeContext);
        } catch (CubeConnectionException e) {
            logger.warn("连接目标异常", e);
            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
            cubeContext.cancelAfterWrite();
        }
    }

    private void handleClientDirectSocksRead(final CubeContext cubeContext,
                                             final ServerContext serverContext,
                                             final ClientRoleContext clientRoleContext) {

        CubeContext targetCubeContext = serverContext.getAssociatedCubeContext();

        // exchange
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = targetCubeContext.readyWrite();
        writeBuffer.put(readBuffer);
    }

    @Override
    public void handleConnectedSuccess(CubeContext cubeContext) {
        ServerContext targetServerContext = (ServerContext) cubeContext.attachment();
        CubeContext clientCubeContext = targetServerContext.getAssociatedCubeContext();

        final byte addressType;
        final byte[] address;
        final short portShort = (short) cubeContext.getLocalPort();

        InetAddress localAddress = cubeContext.getLocalAddress();
        if (localAddress instanceof Inet4Address) {
            addressType = DirectSocksAddressType.IPV4;
            address = localAddress.getAddress();
        } else if (localAddress instanceof Inet6Address) {
            addressType = DirectSocksAddressType.IPV6;
            address = localAddress.getAddress();
        } else {
            logger.error("地址类型不支持");
            cubeContext.cancel();

            final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.GENERAL_FAIL);
            clientCubeContext.cancelAfterWrite();
            return;
        }

        // write
        final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
        writeBuffer.put(DirectSocksVersion.VERSION_1);
        writeBuffer.put(DirectSocksReply.SUCCESS);
        writeBuffer.put(addressType);
        writeBuffer.put(address);
        writeBuffer.putShort(portShort);

        // change state
        ServerContext clientServerContext = (ServerContext) clientCubeContext.attachment();
        ClientRoleContext clientRoleContext = (ClientRoleContext) clientServerContext.getCurrentChannelContext();
        clientRoleContext.setServerState(ServerState.DIRECT_SOCKS);
    }

    @Override
    public void handleConnectedFail(CubeContext cubeContext, CubeConnectionException cubeConnectionException) {
        logger.warn("连接目标异常", cubeConnectionException);
        cubeContext.cancel();

        ServerContext targetServerContext = (ServerContext) cubeContext.attachment();
        CubeContext clientCubeContext = targetServerContext.getAssociatedCubeContext();

        final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
        writeBuffer.put(DirectSocksVersion.VERSION_1);
        writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
        clientCubeContext.cancelAfterWrite();
    }

    private void checkConfig(final ServerConfig serverConfig) throws ServerInitException {
        if (serverConfig == null) throw new ServerInitException("配置不能为空");

        TokenConverter tokenConverter = serverConfig.getTokenConverter();
        if (tokenConverter == null) throw new ServerInitException("TokenConverter不能为空");

        TokenChecker tokenChecker = serverConfig.getTokenChecker();
        if (tokenChecker == null) throw new ServerInitException("TokenChecker不能为空");
    }
}
