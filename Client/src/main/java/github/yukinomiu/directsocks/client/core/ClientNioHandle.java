package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.client.exception.ClientRuntimeException;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
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

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ClientNioHandle.class);

    private final ClientConfig clientConfig;

    public ClientNioHandle(final ClientConfig clientConfig) throws ClientInitException {
        checkConfig(clientConfig);
        this.clientConfig = clientConfig;
    }

    @Override
    public void handleRead(final CubeContext cubeContext) {
        ClientContext clientContext = (ClientContext) cubeContext.attachment();
        if (clientContext == null) {
            clientContext = new ClientContext(ClientChannelRole.LOCAL_ROLE);
            LocalRoleContext localRoleContext = (LocalRoleContext) clientContext.getCurrentChannelContext();
            localRoleContext.setLocalState(LocalState.SOCKS_AUTH);

            cubeContext.attach(clientContext);
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
                throw new ClientRuntimeException(currentRole.name() + "角色不支持");
        }
    }

    private void handleLocalRead(final CubeContext cubeContext, final ClientContext clientContext) {
        final LocalRoleContext localRoleContext = (LocalRoleContext) clientContext.getCurrentChannelContext();
        LocalState currentState = localRoleContext.getLocalState();
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
                throw new ClientRuntimeException(currentState.name() + " 状态不支持");
        }
    }

    private void handleServerRead(final CubeContext cubeContext, final ClientContext clientContext) {
        final ServerRoleContext serverRoleContext = (ServerRoleContext) clientContext.getCurrentChannelContext();
        ServerState currentState = serverRoleContext.getServerState();
        switch (currentState) {
            case DIRECT_SOCKS:
                handleServerDirectSocksRead(cubeContext, clientContext, serverRoleContext);
                break;

            case DIRECT_SOCKS_AUTH:
                handleServerDirectSocksAuthRead(cubeContext, clientContext, serverRoleContext);
                break;

            default:
                throw new ClientRuntimeException(currentState.name() + " 状态不支持");
        }
    }

    private void handleLocalSocksAuthRead(final CubeContext cubeContext,
                                          final ClientContext clientContext,
                                          final LocalRoleContext localRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 3) {
            logger.warn("SOCKS协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS版本 {} 不支持, 断开连接", readBuffer.get(0));
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != readBuffer.remaining()) {
            logger.warn("SOCKS协议错误");
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

        // set context
        if (authSupport) {
            localRoleContext.setAuthMethod(SocksAuthMethod.NO_AUTH);
        } else {
            localRoleContext.setAuthMethod(SocksAuthMethod.NO_ACCEPTABLE_METHODS);
        }

        // check context
        if (localRoleContext.getAuthMethod() == SocksAuthMethod.NO_ACCEPTABLE_METHODS) {
            logger.warn("SOCKS验证协议不支持, 断开连接");

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksAuthMethod.NO_ACCEPTABLE_METHODS);
            cubeContext.cancelAfterWrite();
            return;
        }

        // write
        final ByteBuffer writeBuffer = cubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(SocksAuthMethod.NO_AUTH);

        // change state
        localRoleContext.setLocalState(LocalState.SOCKS_PROXY);
    }

    private void handleLocalSocksProxyRead(final CubeContext cubeContext,
                                           final ClientContext clientContext,
                                           final LocalRoleContext localRoleContext) {

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 4) {
            logger.warn("SOCKS协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksVersion.VERSION_5) {
            logger.warn("SOCKS版本 {} 不支持, 断开连接", readBuffer.get(0));
            cubeContext.cancel();
            return;
        }

        final byte command = readBuffer.get();

        if (command != SocksCommand.CONNECT
                && command != SocksCommand.BIND
                && command != SocksCommand.UDP_ASSOCIATE) {

            logger.warn("SOCKS协议错误");
            cubeContext.cancel();
            return;
        }

        if (readBuffer.get() != SocksReserved.RESERVED) {
            logger.warn("SOCKS协议错误");
            cubeContext.cancel();
            return;
        }

        byte addressType = readBuffer.get();
        byte[] address;
        final byte[] port;

        if (addressType == SocksAddressType.IPV4) {
            if (readBuffer.remaining() != 6) {
                logger.warn("SOCKS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[4];
            port = new byte[2];
            readBuffer.get(address, 0, 4);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksAddressType.DOMAIN_NAME) {
            if (readBuffer.remaining() < 1) {
                logger.warn("SOCKS协议错误");
                cubeContext.cancel();
                return;
            }

            int domainNameLength = readBuffer.get();

            if (readBuffer.remaining() != domainNameLength + 2) {
                logger.warn("SOCKS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[domainNameLength];
            port = new byte[2];
            readBuffer.get(address, 0, domainNameLength);
            readBuffer.get(port, 0, 2);

        } else if (addressType == SocksAddressType.IPV6) {
            if (readBuffer.remaining() != 18) {
                logger.warn("SOCKS协议错误");
                cubeContext.cancel();
                return;
            }

            address = new byte[16];
            port = new byte[2];
            readBuffer.get(address, 0, 16);
            readBuffer.get(port, 0, 2);

        } else {
            logger.warn("SOCKS协议错误");
            cubeContext.cancel();
            return;
        }

        // set context
        if (clientConfig.getLocalDnsResolve() && addressType == SocksAddressType.DOMAIN_NAME) {
            String host = new String(address, StandardCharsets.US_ASCII);
            try {
                InetAddress targetAddress = InetAddress.getByName(host);

                if (targetAddress instanceof Inet4Address) {
                    addressType = SocksAddressType.IPV4;
                    address = targetAddress.getAddress();

                } else if (targetAddress instanceof Inet6Address) {
                    addressType = SocksAddressType.IPV6;
                    address = targetAddress.getAddress();

                } else {
                    throw new ClientRuntimeException("本地解析地址类型不支持");
                }
            } catch (UnknownHostException e) {
                logger.warn("域名 {} 本地解析失败", host);
            }
        }

        localRoleContext.setCommand(command);
        localRoleContext.setAddressType(addressType);
        localRoleContext.setAddress(address);
        localRoleContext.setPort(port);

        // check context
        if (localRoleContext.getCommand() != SocksCommand.CONNECT) {
            logger.warn("命令 {} 不支持, 断开连接", localRoleContext.getCommand());

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.COMMAND_NOT_SUPPORTED);
            cubeContext.cancelAfterWrite();
            return;
        }

        // connect server
        try {
            InetAddress serverAddress = clientConfig.getServerAddress();
            Integer serverPort = clientConfig.getServerPort();
            SocketAddress serverSocketAddress = new InetSocketAddress(serverAddress, serverPort);

            CubeContext serverCubeContext = cubeContext.connectAndRegisterNewSocketChannel(serverSocketAddress);

            ClientContext serverClientContext = new ClientContext(ClientChannelRole.SERVER_ROLE);
            ServerRoleContext serverRoleContext = (ServerRoleContext) serverClientContext.getCurrentChannelContext();
            serverRoleContext.setServerState(ServerState.DIRECT_SOCKS_AUTH);
            serverCubeContext.attach(serverClientContext);

            // associate
            clientContext.setAssociatedCubeContext(serverCubeContext);
            serverClientContext.setAssociatedCubeContext(cubeContext);
        } catch (CubeConnectionException e) {
            logger.warn("连接服务端异常", e);

            final ByteBuffer writeBuffer = cubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.NETWORK_UNREACHABLE);
            cubeContext.cancelAfterWrite();
        }
    }

    private void handleLocalDirectSocksRead(final CubeContext cubeContext,
                                            final ClientContext clientContext,
                                            final LocalRoleContext localRoleContext) {

        CubeContext serverCubeContext = clientContext.getAssociatedCubeContext();

        // exchange
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = serverCubeContext.readyWrite();
        writeBuffer.put(readBuffer);
    }

    private void handleServerDirectSocksRead(final CubeContext cubeContext,
                                             final ClientContext clientContext,
                                             final ServerRoleContext serverRoleContext) {

        CubeContext localCubeContext = clientContext.getAssociatedCubeContext();

        // exchange
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        final ByteBuffer writeBuffer = localCubeContext.readyWrite();
        writeBuffer.put(readBuffer);
    }

    private void handleServerDirectSocksAuthRead(final CubeContext cubeContext,
                                                 final ClientContext clientContext,
                                                 final ServerRoleContext serverRoleContext) {

        CubeContext localCubeContext = clientContext.getAssociatedCubeContext();

        // read
        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        if (readBuffer.remaining() < 2) {
            logger.warn("DS协议错误");
            cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            return;
        }

        if (readBuffer.get() != DirectSocksVersion.VERSION_1) {
            logger.warn("DS版本 {} 不支持", readBuffer.get(0));
            cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            return;
        }

        final byte serverReply = readBuffer.get();
        switch (serverReply) {
            case DirectSocksReply.SUCCESS: {

                if (readBuffer.remaining() < 1) {
                    logger.warn("DS协议错误");
                    cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                    return;
                }

                final byte addressType = readBuffer.get();
                final byte socksAddressType;
                final byte[] address;
                final byte[] port;

                if (addressType == DirectSocksAddressType.IPV4) {
                    if (readBuffer.remaining() != 6) {
                        logger.warn("DS协议错误");
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
                        logger.warn("DS协议错误");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    socksAddressType = SocksAddressType.DOMAIN_NAME;
                    int domainNameLength = readBuffer.get();

                    if (readBuffer.remaining() != domainNameLength + 2) {
                        logger.warn("DS协议错误");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    address = new byte[domainNameLength];
                    port = new byte[2];
                    readBuffer.get(address, 0, domainNameLength);
                    readBuffer.get(port, 0, 2);

                } else if (addressType == DirectSocksAddressType.IPV6) {
                    if (readBuffer.remaining() != 18) {
                        logger.warn("DS协议错误");
                        cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                        return;
                    }

                    socksAddressType = SocksAddressType.IPV6;
                    address = new byte[16];
                    port = new byte[2];
                    readBuffer.get(address, 0, 16);
                    readBuffer.get(port, 0, 2);

                } else {
                    logger.warn("DS地址类型 {} 不支持", addressType);
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

                // change state
                serverRoleContext.setServerState(ServerState.DIRECT_SOCKS);
                ClientContext localClientContext = (ClientContext) localCubeContext.attachment();
                LocalRoleContext localRoleContext = (LocalRoleContext) localClientContext.getCurrentChannelContext();
                localRoleContext.setLocalState(LocalState.DIRECT_SOCKS);
                return;
            }

            case DirectSocksReply.AUTH_FAIL: {
                logger.warn("认证失败");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.CONNECTION_NOT_ALLOWED);
                return;
            }

            case DirectSocksReply.CONNECTION_TYPE_NOT_SUPPORTED: {
                logger.warn("服务端连接类型不支持");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.COMMAND_NOT_SUPPORTED);
                return;
            }

            case DirectSocksReply.NETWORK_UNREACHABLE: {
                logger.warn("网络不可达");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.NETWORK_UNREACHABLE);
                return;
            }

            case DirectSocksReply.HOST_UNREACHABLE: {
                logger.warn("主机不可达");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.HOST_UNREACHABLE);
                return;
            }

            case DirectSocksReply.GENERAL_FAIL: {
                logger.warn("服务端错误");
                cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
                return;
            }

            default: {
                logger.warn("服务端回复状态 {} 不支持", serverReply);
                cancelAndReply(cubeContext, localCubeContext, SocksReply.GENERAL_SERVER_FAILURE);
            }
        }
    }

    @Override
    public void handleConnectedSuccess(CubeContext cubeContext) {
        ClientContext serverClientContext = (ClientContext) cubeContext.attachment();
        CubeContext localCubeContext = serverClientContext.getAssociatedCubeContext();
        ClientContext localClientContext = (ClientContext) localCubeContext.attachment();
        LocalRoleContext localRoleContext = (LocalRoleContext) localClientContext.getCurrentChannelContext();

        // token
        TokenConverter tokenConverter = clientConfig.getTokenConverter();
        byte[] token = tokenConverter.convertToken(clientConfig.getKey());

        // connection type
        byte connectionType;
        if (localRoleContext.getCommand() == SocksCommand.CONNECT) {
            connectionType = DirectSocksConnectionType.TCP;
        } else {
            logger.warn("命令 {} 不支持, 断开连接", localRoleContext.getCommand());
            cubeContext.cancel();

            final ByteBuffer writeBuffer = localCubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.COMMAND_NOT_SUPPORTED);
            localCubeContext.cancelAfterWrite();
            return;
        }

        // address type
        byte addressType;
        if (localRoleContext.getAddressType() == SocksAddressType.IPV4) {
            addressType = DirectSocksAddressType.IPV4;
        } else if (localRoleContext.getAddressType() == SocksAddressType.DOMAIN_NAME) {
            addressType = DirectSocksAddressType.DOMAIN_NAME;
        } else if (localRoleContext.getAddressType() == SocksAddressType.IPV6) {
            addressType = DirectSocksAddressType.IPV6;
        } else {
            logger.warn("地址类型 {} 不支持, 断开连接", localRoleContext.getAddressType());
            cubeContext.cancel();

            final ByteBuffer writeBuffer = localCubeContext.readyWrite();
            writeBuffer.put(SocksVersion.VERSION_5);
            writeBuffer.put(SocksReply.ADDRESS_TYPE_NOT_SUPPORTED);
            localCubeContext.cancelAfterWrite();
            return;
        }

        // address
        byte[] address = localRoleContext.getAddress();
        byte[] port = localRoleContext.getPort();

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
    }

    @Override
    public void handleConnectedFail(CubeContext cubeContext, CubeConnectionException cubeConnectionException) {
        logger.warn("连接服务端异常", cubeConnectionException);
        cubeContext.cancel();

        ClientContext serverClientContext = (ClientContext) cubeContext.attachment();
        CubeContext localCubeContext = serverClientContext.getAssociatedCubeContext();

        final ByteBuffer writeBuffer = localCubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(SocksReply.GENERAL_SERVER_FAILURE);
        localCubeContext.cancelAfterWrite();
    }

    private void cancelAndReply(final CubeContext serverCubeContext, final CubeContext localCubeContext, final byte reply) {
        serverCubeContext.cancel();

        final ByteBuffer writeBuffer = localCubeContext.readyWrite();
        writeBuffer.put(SocksVersion.VERSION_5);
        writeBuffer.put(reply);
        localCubeContext.cancelAfterWrite();
    }

    private void checkConfig(final ClientConfig clientConfig) throws ClientInitException {
        if (clientConfig == null) throw new ClientInitException("配置不能为空");

        Boolean localDnsResolve = clientConfig.getLocalDnsResolve();
        if (localDnsResolve == null) throw new ClientInitException("DNS本地解析配置不能为空");

        InetAddress serverAddress = clientConfig.getServerAddress();
        if (serverAddress == null) throw new ClientInitException("服务端地址不能为空");
        if (!(serverAddress instanceof Inet4Address) && !(serverAddress instanceof Inet6Address))
            throw new ClientInitException("服务端地址类型不支持, 仅支持 IPv4 和 IPv6 地址");

        Integer serverPort = clientConfig.getServerPort();
        if (serverPort == null) throw new ClientInitException("服务端端口不能为空");
        if (serverPort < 1 || serverPort > 65535) throw new ClientInitException("服务端端口非法, 端口必须在[1, 65535]之间取值");

        TokenConverter tokenConverter = clientConfig.getTokenConverter();
        if (tokenConverter == null) throw new ClientInitException("TokenConverter不能为空");

        String key = clientConfig.getKey();
        if (key == null) throw new ClientInitException("密钥不能为空");
    }
}
