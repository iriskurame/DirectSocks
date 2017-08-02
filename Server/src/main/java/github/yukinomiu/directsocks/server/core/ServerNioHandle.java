package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.auth.TokenGenerator;
import github.yukinomiu.directsocks.common.auth.TokenVerifier;
import github.yukinomiu.directsocks.common.crypto.Crypto;
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
public final class ServerNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ServerNioHandle.class);

    private final ServerConfig serverConfig;

    ServerNioHandle(final ServerConfig serverConfig) throws ServerInitException {
        checkConfig(serverConfig);
        this.serverConfig = serverConfig;
    }

    @Override
    public void handleRead(CubeContext cubeContext) {
        ServerContext serverContext = (ServerContext) cubeContext.attachment();
        if (serverContext == null) {
            serverContext = new ServerContext(ServerChannelRole.CLIENT_ROLE);
            cubeContext.attach(serverContext);

            ClientRoleContext clientRoleContext = (ClientRoleContext) serverContext.getCurrentChannelContext();
            clientRoleContext.setServerState(ServerState.DIRECT_SOCKS_AUTH);
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
                logger.error("ServerChannelRole not supported: {}", currentRole.name());
                throw new ServerRuntimeException("ServerChannelRole not supported");
        }
    }

    @Override
    public void handleConnectSuccess(CubeContext cubeContext) {
        final ServerContext targetServerContext = (ServerContext) cubeContext.attachment();
        final CubeContext clientCubeContext = targetServerContext.getAssociatedCubeContext();

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
            logger.error("local address type not supported");
            cubeContext.close();

            final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.GENERAL_FAIL);
            clientCubeContext.closeAfterWrite();
            return;
        }

        // write
        final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
        writeBuffer.put(DirectSocksVersion.VERSION_1);
        writeBuffer.put(DirectSocksReply.SUCCESS);
        writeBuffer.put(addressType);
        writeBuffer.put(address);
        writeBuffer.putShort(portShort);
        clientCubeContext.readAfterWrite();

        // change state
        ServerContext clientServerContext = (ServerContext) clientCubeContext.attachment();
        ClientRoleContext clientRoleContext = (ClientRoleContext) clientServerContext.getCurrentChannelContext();
        clientRoleContext.setServerState(ServerState.DIRECT_SOCKS);
    }

    @Override
    public void handleConnectFail(CubeContext cubeContext, CubeConnectionException cubeConnectionException) {
        logger.warn("connect to target exception: {}", cubeConnectionException.getMessage());
        cubeContext.close();

        final ServerContext targetServerContext = (ServerContext) cubeContext.attachment();
        CubeContext clientCubeContext = targetServerContext.getAssociatedCubeContext();

        final ByteBuffer writeBuffer = clientCubeContext.readyWrite();
        writeBuffer.put(DirectSocksVersion.VERSION_1);
        writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
        clientCubeContext.closeAfterWrite();
    }

    private void handleClientRead(final CubeContext cubeContext, final ServerContext serverContext) {
        final ClientRoleContext clientRoleContext = (ClientRoleContext) serverContext.getCurrentChannelContext();
        final ServerState currentState = clientRoleContext.getServerState();
        switch (currentState) {
            case DIRECT_SOCKS_AUTH:
                handleClientDirectSocksAuthRead(cubeContext, serverContext, clientRoleContext);
                break;

            case DIRECT_SOCKS:
                handleClientDirectSocksRead(cubeContext, serverContext, clientRoleContext);
                break;

            default:
                logger.error("ServerState not supported: {}" + currentState.name());
                throw new ServerRuntimeException("ServerState not supported");

        }
    }

    private void handleTargetRead(final CubeContext cubeContext, final ServerContext serverContext) {
        final CubeContext clientCubeContext = serverContext.getAssociatedCubeContext();
        final Crypto crypto = serverConfig.getCrypto();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = clientCubeContext.readyWrite();

        // encrypt and exchange
        try {
            crypto.encrypt(readBuffer, writeBuffer);
        } catch (Exception e) {
            logger.warn("server encrypt exception: {}", e.getMessage());
            cubeContext.close();
            clientCubeContext.close();
            return;
        }

        clientCubeContext.readAfterWrite();
        clientCubeContext.setAfterWriteCallback(arg -> cubeContext.readyRead(), null);
    }

    private void handleClientDirectSocksAuthRead(final CubeContext cubeContext,
                                                 final ServerContext serverContext,
                                                 final ClientRoleContext clientRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final int authLength = serverConfig.getTokenGenerator().targetLength();

        if (readBuffer.remaining() < authLength + 3) {
            logger.warn("DS error");
            cubeContext.close();
            return;
        }

        if (readBuffer.get() != DirectSocksVersion.VERSION_1) {
            logger.warn("DS version not supported, version: {}", readBuffer.get(0));
            cubeContext.close();
            return;
        }

        final byte[] token = new byte[authLength];
        readBuffer.get(token, 0, authLength);

        final byte connectionType = readBuffer.get();
        if (connectionType != DirectSocksConnectionType.TCP && connectionType != DirectSocksConnectionType.UDP) {
            logger.warn("DS error");
            cubeContext.close();
            return;
        }

        final byte addressType = readBuffer.get();
        final byte[] address;
        final short portShort;

        if (addressType == DirectSocksAddressType.IPV4) {
            if (readBuffer.remaining() != 6) {
                logger.warn("DS error");
                cubeContext.close();
                return;
            }

            address = new byte[4];
            readBuffer.get(address, 0, 4);
            portShort = readBuffer.getShort();

        } else if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
            if (readBuffer.remaining() < 1) {
                logger.warn("DS error");
                cubeContext.close();
                return;
            }

            int domainNameLength = readBuffer.get();

            if (readBuffer.remaining() != domainNameLength + 2) {
                logger.warn("DS error");
                cubeContext.close();
                return;
            }

            address = new byte[domainNameLength];
            readBuffer.get(address, 0, domainNameLength);
            portShort = readBuffer.getShort();

        } else if (addressType == DirectSocksAddressType.IPV6) {
            if (readBuffer.remaining() != 18) {
                logger.warn("DS error");
                cubeContext.close();
                return;
            }

            address = new byte[16];
            readBuffer.get(address, 0, 16);
            portShort = readBuffer.getShort();

        } else {
            logger.warn("DS error");
            cubeContext.close();
            return;
        }

        // set context
        clientRoleContext.setToken(token);
        clientRoleContext.setConnectionType(connectionType);
        clientRoleContext.setAddressType(addressType);
        clientRoleContext.setAddress(address);
        clientRoleContext.setPort(portShort);

        // verify context
        boolean authSuccess = serverConfig.getTokenVerifier().verify(token);
        if (!authSuccess) {
            if (logger.isInfoEnabled()) {
                String remoteAddressString = cubeContext.getRemoteAddress().getHostAddress();
                String remotePortString = String.valueOf(cubeContext.getRemotePort());
                logger.info("token auth fail, remote address: {}:{}", remoteAddressString, remotePortString);
            }

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.AUTH_FAIL);
            cubeContext.closeAfterWrite();
            return;
        }

        if (connectionType != DirectSocksConnectionType.TCP) {
            logger.info("connection type not supported, connection type: {}", connectionType);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.CONNECTION_TYPE_NOT_SUPPORTED);
            cubeContext.closeAfterWrite();
            return;
        }

        // connect target
        String targetHost = null;
        final InetAddress targetAddress; // resolve target address
        try {
            if (addressType == DirectSocksAddressType.IPV4) {
                targetAddress = InetAddress.getByAddress(address);

            } else if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
                targetHost = new String(address, StandardCharsets.US_ASCII);
                targetAddress = InetAddress.getByName(targetHost);

            } else if (addressType == DirectSocksAddressType.IPV6) {
                targetAddress = InetAddress.getByAddress(address);

            } else {
                throw new ServerRuntimeException("target address type not supported");
            }
        } catch (UnknownHostException e) {
            logger.info("resolving host name fail: {}", targetHost);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
            cubeContext.closeAfterWrite();
            return;
        }

        // connect target
        SocketAddress targetSocketAddress = new InetSocketAddress(targetAddress, (int) portShort);

        CubeContext targetCubeContext;
        try {
            targetCubeContext = cubeContext.readyConnect(targetSocketAddress);
        } catch (CubeConnectionException e) {
            logger.info("connect to target exception: {}", e.getMessage());

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(DirectSocksVersion.VERSION_1);
            writeBuffer.put(DirectSocksReply.HOST_UNREACHABLE);
            cubeContext.closeAfterWrite();
            return;
        }

        ServerContext targetServerContext = new ServerContext(ServerChannelRole.TARGET_ROLE);
        targetCubeContext.attach(targetServerContext);

        // associate
        serverContext.setAssociatedCubeContext(targetCubeContext);
        targetServerContext.setAssociatedCubeContext(cubeContext);
    }

    private void handleClientDirectSocksRead(final CubeContext cubeContext,
                                             final ServerContext serverContext,
                                             final ClientRoleContext clientRoleContext) {

        final CubeContext targetCubeContext = serverContext.getAssociatedCubeContext();
        final Crypto crypto = serverConfig.getCrypto();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = targetCubeContext.readyWrite();
        final ByteBuffer frameBuffer = cubeContext.getFrameBuffer();

        // decrypt and exchange
        boolean process;
        try {
            process = crypto.decrypt(frameBuffer, readBuffer, writeBuffer);
        } catch (Exception e) {
            logger.warn("server decrypt exception: {}", e.getMessage());
            cubeContext.close();
            targetCubeContext.close();
            return;
        }

        if (process) {
            targetCubeContext.readAfterWrite();
            targetCubeContext.setAfterWriteCallback(arg -> cubeContext.readyRead(), null);
        } else {
            targetCubeContext.cancelReadyWrite();
            cubeContext.readyRead();
        }
    }

    private void checkConfig(final ServerConfig serverConfig) throws ServerInitException {
        if (serverConfig == null) throw new ServerInitException("config can not be null");

        TokenGenerator tokenGenerator = serverConfig.getTokenGenerator();
        if (tokenGenerator == null) throw new ServerInitException("TokenGenerator can not be null");

        TokenVerifier tokenVerifier = serverConfig.getTokenVerifier();
        if (tokenVerifier == null) throw new ServerInitException("TokenVerifier can not be null");

        String secret = serverConfig.getSecret();
        if (secret == null) throw new ServerInitException("Secret can not be null");

        Crypto crypto = serverConfig.getCrypto();
        if (crypto == null) throw new ServerInitException("Crypto can not be null");
    }
}
