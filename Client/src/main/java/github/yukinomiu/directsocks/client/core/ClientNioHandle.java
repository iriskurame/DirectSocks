package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.client.exception.ClientRuntimeException;
import github.yukinomiu.directsocks.common.auth.TokenGenerator;
import github.yukinomiu.directsocks.common.crypto.Crypto;
import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;
import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
import github.yukinomiu.directsocks.common.protocol.DirectSocksAddressType;
import github.yukinomiu.directsocks.common.protocol.DirectSocksConnectionType;
import github.yukinomiu.directsocks.common.protocol.DirectSocksReply;
import github.yukinomiu.directsocks.common.protocol.DirectSocksVersion;
import github.yukinomiu.directsocks.common.rfc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.ShortBufferException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * Yukinomiu
 * 2017/7/24
 */
public final class ClientNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ClientNioHandle.class);

    private final ClientConfig clientConfig;

    ClientNioHandle(final ClientConfig clientConfig) throws ClientInitException {
        checkConfig(clientConfig);
        this.clientConfig = clientConfig;
    }

    @Override
    public void handleRead(final CubeContext cubeContext) {
        ClientContext clientContext = (ClientContext) cubeContext.attachment();
        if (clientContext == null) {
            clientContext = new ClientContext(ClientChannelRole.LOCAL_ROLE);
            cubeContext.attach(clientContext);

            LocalRoleContext localRoleContext = (LocalRoleContext) clientContext.getCurrentChannelContext();
            localRoleContext.setLocalState(LocalState.SOCKS_AUTH);
        }

        final ClientChannelRole currentRole = clientContext.getClientChannelRole();
        switch (currentRole) {
            case LOCAL_ROLE:
                handleLocalRead(cubeContext, clientContext);
                break;

            case SERVER_ROLE:
                handleServerRead(cubeContext, clientContext);
                break;

            default:
                logger.error("ClientChannelRole not supported: {}", currentRole.name());
                throw new ClientRuntimeException("ClientChannelRole not supported");
        }
    }

    @Override
    public void handleConnectSuccess(CubeContext cubeContext) {
        final ClientContext serverClientContext = (ClientContext) cubeContext.attachment();
        final CubeContext localCubeContext = serverClientContext.getAssociatedCubeContext();
        final ClientContext localClientContext = (ClientContext) localCubeContext.attachment();
        final LocalRoleContext localRoleContext = (LocalRoleContext) localClientContext.getCurrentChannelContext();

        // token
        byte[] token = this.clientConfig.getTokenGenerator().generate(clientConfig.getKey());

        // connection type
        byte connectionType;
        if (localRoleContext.getCommand() == SocksCommand.CONNECT) {
            connectionType = DirectSocksConnectionType.TCP;
        } else {
            logger.warn("SOCKS command not supported, command: {}", localRoleContext.getCommand());
            cubeContext.close();

            final ByteBuffer writeBuffer = localCubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.COMMAND_NOT_SUPPORTED);
            localCubeContext.closeAfterWrite();
            return;
        }

        // address type
        final byte addressType;
        if (localRoleContext.getAddressType() == SocksAddressType.IPV4) {
            addressType = DirectSocksAddressType.IPV4;

        } else if (localRoleContext.getAddressType() == SocksAddressType.DOMAIN_NAME) {
            addressType = DirectSocksAddressType.DOMAIN_NAME;

        } else if (localRoleContext.getAddressType() == SocksAddressType.IPV6) {
            addressType = DirectSocksAddressType.IPV6;

        } else {
            logger.warn("SOCKS address type not supported, address type: {}", localRoleContext.getAddressType());
            cubeContext.close();

            final ByteBuffer writeBuffer = localCubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.ADDRESS_TYPE_NOT_SUPPORTED);
            localCubeContext.closeAfterWrite();
            return;
        }

        // address
        final byte[] address = localRoleContext.getAddress();
        final byte[] port = localRoleContext.getPort();

        // write
        final ByteBuffer writeBuffer = cubeContext.readyWrite();
        writeBuffer.put(DirectSocksVersion.VERSION_1);
        writeBuffer.put(token);
        writeBuffer.put(connectionType);
        writeBuffer.put(addressType);
        if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
            writeBuffer.put((byte) address.length);
        }
        writeBuffer.put(address);
        writeBuffer.put(port);
        cubeContext.readAfterWrite();
    }

    @Override
    public void handleConnectFail(CubeContext cubeContext, CubeConnectionException cubeConnectionException) {
        logger.warn("connect to server exception: {}", cubeConnectionException.getMessage());
        cubeContext.close();

        final ClientContext serverClientContext = (ClientContext) cubeContext.attachment();
        CubeContext localCubeContext = serverClientContext.getAssociatedCubeContext();

        final ByteBuffer writeBuffer = localCubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(SocksReply.GENERAL_SERVER_FAILURE);
        localCubeContext.closeAfterWrite();
    }

    private void handleLocalRead(final CubeContext cubeContext, final ClientContext clientContext) {
        final LocalRoleContext localRoleContext = (LocalRoleContext) clientContext.getCurrentChannelContext();
        final LocalState currentState = localRoleContext.getLocalState();
        switch (currentState) {
            case SOCKS_AUTH:
                handleLocalSocksAuthRead(cubeContext, clientContext, localRoleContext);
                break;

            case SOCKS_PROXY:
                handleLocalSocksProxyRead(cubeContext, clientContext, localRoleContext);
                break;

            case DIRECT_SOCKS:
                handleLocalDirectSocksRead(cubeContext, clientContext, localRoleContext);
                break;

            default:
                logger.error("LocalState not supported: {}", currentState.name());
                throw new ClientRuntimeException("LocalState not supported");
        }
    }

    private void handleServerRead(final CubeContext cubeContext, final ClientContext clientContext) {
        final ServerRoleContext serverRoleContext = (ServerRoleContext) clientContext.getCurrentChannelContext();
        final ServerState currentState = serverRoleContext.getServerState();
        switch (currentState) {
            case DIRECT_SOCKS:
                handleServerDirectSocksRead(cubeContext, clientContext, serverRoleContext);
                break;

            case DIRECT_SOCKS_AUTH:
                handleServerDirectSocksAuthRead(cubeContext, clientContext, serverRoleContext);
                break;

            default:
                logger.error("ServerState not supported: {}", currentState.name());
                throw new ClientRuntimeException("ServerState not supported");
        }
    }

    private void handleLocalSocksAuthRead(final CubeContext cubeContext,
                                          final ClientContext clientContext,
                                          final LocalRoleContext localRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 3) {
            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS version not supported, version: {}", readBuffer.get(0));
            cubeContext.close();
            return;
        }

        if (readBuffer.get() != readBuffer.remaining()) {
            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        boolean authSupport = false;
        while (readBuffer.hasRemaining()) {
            if (readBuffer.get() == SocksAuthMethod.NO_AUTH) {
                authSupport = true;
                break;
            }
        }

        // set context
        if (authSupport) {
            localRoleContext.setAuthMethod(SocksAuthMethod.NO_AUTH);
        } else {
            localRoleContext.setAuthMethod(SocksAuthMethod.NO_ACCEPTABLE_METHODS);
        }

        // verify context
        if (localRoleContext.getAuthMethod() == SocksAuthMethod.NO_ACCEPTABLE_METHODS) {
            logger.warn("SOCKS auth method not supported");

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksAuthMethod.NO_ACCEPTABLE_METHODS);
            cubeContext.closeAfterWrite();
            return;
        }

        // write
        final ByteBuffer writeBuffer = cubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(SocksAuthMethod.NO_AUTH);
        cubeContext.readAfterWrite();

        // change state
        localRoleContext.setLocalState(LocalState.SOCKS_PROXY);
    }

    private void handleLocalSocksProxyRead(final CubeContext cubeContext,
                                           final ClientContext clientContext,
                                           final LocalRoleContext localRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 4) {
            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS version not supported, version: {}", readBuffer.get(0));
            cubeContext.close();
            return;
        }

        final byte command = readBuffer.get();
        if (command != SocksCommand.CONNECT
                && command != SocksCommand.BIND
                && command != SocksCommand.UDP_ASSOCIATE) {

            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        if (readBuffer.get() != SocksReserved.RESERVED) {
            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        byte addressType = readBuffer.get();
        byte[] address;
        final byte[] port;

        if (addressType == SocksAddressType.IPV4) {
            if (readBuffer.remaining() != 6) {
                logger.warn("SOCKS error");
                cubeContext.close();
                return;
            }

            address = new byte[4];
            port = new byte[2];
            readBuffer.get(address, 0, 4);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksAddressType.DOMAIN_NAME) {
            if (readBuffer.remaining() < 1) {
                logger.warn("SOCKS error");
                cubeContext.close();
                return;
            }

            int domainNameLength = readBuffer.get();
            if (domainNameLength + 7 > readBuffer.capacity()) {
                logger.warn("read buffer size too small to support long domain name");
                final ByteBuffer byteBuffer = cubeContext.readyWrite();
                byteBuffer.put(SocksVersion.VERSION_5);
                byteBuffer.put(SocksReply.GENERAL_SERVER_FAILURE);
                cubeContext.closeAfterWrite();
                return;
            }

            if (readBuffer.remaining() != domainNameLength + 2) {
                logger.warn("SOCKS error");
                cubeContext.close();
                return;
            }

            address = new byte[domainNameLength];
            port = new byte[2];
            readBuffer.get(address, 0, domainNameLength);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksAddressType.IPV6) {
            if (readBuffer.remaining() != 18) {
                logger.warn("SOCKS error");
                cubeContext.close();
                return;
            }

            address = new byte[16];
            port = new byte[2];
            readBuffer.get(address, 0, 16);
            readBuffer.get(port, 0, 2);

        } else {
            logger.warn("SOCKS error");
            cubeContext.close();
            return;
        }

        // local DNS resolve
        if (clientConfig.getLocalDnsResolve() && addressType == SocksAddressType.DOMAIN_NAME) {
            String targetHost = new String(address, StandardCharsets.US_ASCII);
            try {
                InetAddress targetAddress = InetAddress.getByName(targetHost); // resolve

                if (targetAddress instanceof Inet4Address) {
                    addressType = SocksAddressType.IPV4;
                    address = targetAddress.getAddress();

                } else if (targetAddress instanceof Inet6Address) {
                    addressType = SocksAddressType.IPV6;
                    address = targetAddress.getAddress();

                } else {
                    throw new ClientRuntimeException("resolved address type not supported");
                }
            } catch (UnknownHostException e) {
                logger.warn("resolving host name fail, host name: {}", targetHost);
            }
        }

        // set context
        localRoleContext.setCommand(command);
        localRoleContext.setAddressType(addressType);
        localRoleContext.setAddress(address);
        localRoleContext.setPort(port);

        // verify context
        if (localRoleContext.getCommand() != SocksCommand.CONNECT) {
            logger.warn("SOCKS command not supported, command: {}", localRoleContext.getCommand());

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.COMMAND_NOT_SUPPORTED);
            cubeContext.closeAfterWrite();
            return;
        }

        // connect server
        InetAddress serverAddress = clientConfig.getServerAddress();
        Integer serverPort = clientConfig.getServerPort();
        SocketAddress serverSocketAddress = new InetSocketAddress(serverAddress, serverPort);

        CubeContext serverCubeContext;
        try {
            serverCubeContext = cubeContext.readyConnect(serverSocketAddress);
        } catch (CubeConnectionException e) {
            logger.error("connect to server exception: {}", e.getMessage());

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.NETWORK_UNREACHABLE);
            cubeContext.closeAfterWrite();
            return;
        }

        ClientContext serverClientContext = new ClientContext(ClientChannelRole.SERVER_ROLE);
        serverCubeContext.attach(serverClientContext);
        ServerRoleContext serverRoleContext = (ServerRoleContext) serverClientContext.getCurrentChannelContext();
        serverRoleContext.setServerState(ServerState.DIRECT_SOCKS_AUTH);

        // associate
        clientContext.setAssociatedCubeContext(serverCubeContext);
        serverClientContext.setAssociatedCubeContext(cubeContext);
    }

    private void handleLocalDirectSocksRead(final CubeContext cubeContext,
                                            final ClientContext clientContext,
                                            final LocalRoleContext localRoleContext) {

        final CubeContext serverCubeContext = clientContext.getAssociatedCubeContext();
        final Crypto crypto = clientConfig.getCrypto();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = serverCubeContext.readyWrite();

        // encrypt and exchange
        try {
            crypto.encrypt(readBuffer, writeBuffer);
        } catch (ShortBufferException | CryptoException e) {
            logger.error("client encrypt exception", e);
            cubeContext.close();
            serverCubeContext.close();
            return;
        }

        serverCubeContext.readAfterWrite();
        serverCubeContext.setAfterWriteCallback(arg -> cubeContext.readyRead(), null);
    }

    private void handleServerDirectSocksRead(final CubeContext cubeContext,
                                             final ClientContext clientContext,
                                             final ServerRoleContext serverRoleContext) {

        final CubeContext localCubeContext = clientContext.getAssociatedCubeContext();
        final Crypto crypto = clientConfig.getCrypto();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = localCubeContext.readyWrite();

        // decrypt and exchange
        try {
            crypto.decrypt(readBuffer, writeBuffer);
        } catch (ShortBufferException | CryptoException e) {
            logger.error("client decrypt exception", e);
            cubeContext.close();
            localCubeContext.close();
            return;
        }

        localCubeContext.readAfterWrite();
        localCubeContext.setAfterWriteCallback(arg -> cubeContext.readyRead(), null);
    }

    private void handleServerDirectSocksAuthRead(final CubeContext cubeContext,
                                                 final ClientContext clientContext,
                                                 final ServerRoleContext serverRoleContext) {

        final CubeContext localCubeContext = clientContext.getAssociatedCubeContext();

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 2) {
            logger.warn("DS error");
            cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            return;
        }

        if (readBuffer.get() != DirectSocksVersion.VERSION_1) {
            logger.warn("DS version not supported, version: {}", readBuffer.get(0));
            cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            return;
        }

        final byte serverReply = readBuffer.get();
        switch (serverReply) {
            case DirectSocksReply.SUCCESS: {

                if (readBuffer.remaining() < 1) {
                    logger.warn("DS error");
                    cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                    return;
                }

                final byte addressType = readBuffer.get();
                final byte socksAddressType;
                final byte[] address;
                final byte[] port;

                if (addressType == DirectSocksAddressType.IPV4) {
                    if (readBuffer.remaining() != 6) {
                        logger.warn("DS error");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    socksAddressType = SocksAddressType.IPV4;
                    address = new byte[4];
                    port = new byte[2];
                    readBuffer.get(address, 0, 4);
                    readBuffer.get(port, 0, 2);

                } else if (addressType == DirectSocksAddressType.DOMAIN_NAME) {
                    if (readBuffer.remaining() < 1) {
                        logger.warn("DS error");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    socksAddressType = SocksAddressType.DOMAIN_NAME;
                    int domainNameLength = readBuffer.get();

                    if (readBuffer.remaining() != domainNameLength + 2) {
                        logger.warn("DS error");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    address = new byte[domainNameLength];
                    port = new byte[2];
                    readBuffer.get(address, 0, domainNameLength);
                    readBuffer.get(port, 0, 2);

                } else if (addressType == DirectSocksAddressType.IPV6) {
                    if (readBuffer.remaining() != 18) {
                        logger.warn("DS error");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    socksAddressType = SocksAddressType.IPV6;
                    address = new byte[16];
                    port = new byte[2];
                    readBuffer.get(address, 0, 16);
                    readBuffer.get(port, 0, 2);

                } else {
                    logger.warn("DS address type not supported, address type: ", addressType);
                    cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                    return;
                }

                // write
                final ByteBuffer writeBuffer = localCubeContext.readyWrite();
                writeBuffer.put(SocksVersion.VERSION_5);
                writeBuffer.put(SocksReply.SUCCEEDED);
                writeBuffer.put(SocksReserved.RESERVED);
                writeBuffer.put(socksAddressType);
                writeBuffer.put(address);
                writeBuffer.put(port);
                localCubeContext.readAfterWrite();

                // change state
                serverRoleContext.setServerState(ServerState.DIRECT_SOCKS);
                ClientContext localClientContext = (ClientContext) localCubeContext.attachment();
                LocalRoleContext localRoleContext = (LocalRoleContext) localClientContext.getCurrentChannelContext();
                localRoleContext.setLocalState(LocalState.DIRECT_SOCKS);
                return;
            }

            case DirectSocksReply.AUTH_FAIL: {
                logger.warn("server error: auth fail");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.CONNECTION_NOT_ALLOWED);
                return;
            }

            case DirectSocksReply.CONNECTION_TYPE_NOT_SUPPORTED: {
                logger.warn("server error: connection type not supported");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.COMMAND_NOT_SUPPORTED);
                return;
            }

            case DirectSocksReply.NETWORK_UNREACHABLE: {
                logger.warn("server error: network unreachable");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.NETWORK_UNREACHABLE);
                return;
            }

            case DirectSocksReply.HOST_UNREACHABLE: {
                logger.warn("server error: host unreachable");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.HOST_UNREACHABLE);
                return;
            }

            case DirectSocksReply.GENERAL_FAIL: {
                logger.warn("server error: general exception");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                return;
            }

            default: {
                logger.warn("server reply not supported, server reply: {}", serverReply);
                cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            }
        }
    }

    private void cancelAndReply(final CubeContext serverCubeContext, final CubeContext localCubeContext, final byte reply) {
        serverCubeContext.close();

        final ByteBuffer writeBuffer = localCubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(reply);
        localCubeContext.closeAfterWrite();
    }

    private void checkConfig(final ClientConfig clientConfig) throws ClientInitException {
        if (clientConfig == null) throw new ClientInitException("config can not be null");

        Boolean localDnsResolve = clientConfig.getLocalDnsResolve();
        if (localDnsResolve == null) throw new ClientInitException("LocalDnsResolve can not be null");

        InetAddress serverAddress = clientConfig.getServerAddress();
        if (serverAddress == null) throw new ClientInitException("ServerAddress can not be null");
        if (!(serverAddress instanceof Inet4Address) && !(serverAddress instanceof Inet6Address))
            throw new ClientInitException("ServerAddress type not supported");

        Integer serverPort = clientConfig.getServerPort();
        if (serverPort == null) throw new ClientInitException("ServerPort can not be null");
        if (serverPort < 1 || serverPort > 65535) throw new ClientInitException("ServerPort must in range 1-65535");

        TokenGenerator tokenGenerator = clientConfig.getTokenGenerator();
        if (tokenGenerator == null) throw new ClientInitException("TokenGenerator can not be null");

        String key = clientConfig.getKey();
        if (key == null) throw new ClientInitException("Key can not be null");

        String secret = clientConfig.getSecret();
        if (secret == null) throw new ClientInitException("Secret can not be null");

        Crypto crypto = clientConfig.getCrypto();
        if (crypto == null) throw new ClientInitException("Crypto can not be null");
    }
}
