package github.yukinomiu.directsocks.client.server;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.common.rfc.SocksAuthMethod;
import github.yukinomiu.directsocks.common.rfc.SocksRequest;
import github.yukinomiu.directsocks.common.util.ByteUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientContext implements CloseableAttachment {
    private final ClientChannelRole clientChannelRole;
    private ClientProxyState clientProxyState;

    private Byte socksAuthMethod;
    private Byte command;
    private Byte addressType;
    private byte[] address;
    private byte[] port;

    public ClientContext(final ClientChannelRole clientChannelRole) {
        this.clientChannelRole = clientChannelRole;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public String toString() {
        String socksAuthMethodStr;
        if (socksAuthMethod == null) {
            socksAuthMethodStr = "NULL";
        } else {
            switch (socksAuthMethod) {
                case SocksAuthMethod.NO_AUTH:
                    socksAuthMethodStr = "NO_AUTH";
                    break;

                default:
                    socksAuthMethodStr = "UNKNOWN";
            }
        }

        String commandStr;
        if (command == null) {
            commandStr = "NULL";
        } else {
            switch (command) {
                case SocksRequest.COMMAND_CONNECT:
                    commandStr = "CONNECT";
                    break;

                case SocksRequest.COMMAND_BIND:
                    commandStr = "BIND";
                    break;

                case SocksRequest.COMMAND_UDP_ASSOCIATE:
                    commandStr = "UDP ASSOCIATE";
                    break;

                default:
                    commandStr = "UNKNOWN";
            }
        }

        String addressTypeStr;
        String addressStr;
        if (addressType == null || address == null) {
            addressTypeStr = "NULL";
            addressStr = "NULL";
        } else {
            switch (addressType) {
                case SocksRequest.ATYP_IPV4:
                    addressTypeStr = "IPv4";
                    try {
                        addressStr = InetAddress.getByAddress(address).getHostAddress();
                    } catch (UnknownHostException e) {
                        addressStr = "ERROR";
                    }
                    break;

                case SocksRequest.ATYP_DOMAIN_NAME:
                    addressTypeStr = "DomainName";
                    addressStr = new String(address, StandardCharsets.US_ASCII);
                    break;

                case SocksRequest.ATYP_IPV6:
                    addressTypeStr = "IPv6";
                    try {
                        addressStr = InetAddress.getByAddress(address).getHostAddress();
                    } catch (UnknownHostException e) {
                        addressStr = "ERROR";
                    }
                    break;

                default:
                    addressTypeStr = "UNKNOWN";
                    addressStr = "UNKNOWN";
            }
        }

        String portStr;
        if (port == null) {
            portStr = "NULL";
        } else {
            portStr = Integer.toString(ByteUtil.bytesToInt(port, 0, port.length - 1));
        }

        return "ClientContext{" +
                "clientChannelRole=" + clientChannelRole +
                ", clientProxyState=" + clientProxyState +
                ", socksAuthMethod=" + socksAuthMethodStr +
                ", command=" + commandStr +
                ", addressType=" + addressTypeStr +
                ", address=" + addressStr +
                ", port=" + portStr +
                '}';
    }

    public ClientChannelRole getClientChannelRole() {
        return clientChannelRole;
    }

    public ClientProxyState getClientProxyState() {
        return clientProxyState;
    }

    public void setClientProxyState(ClientProxyState clientProxyState) {
        this.clientProxyState = clientProxyState;
    }

    public Byte getSocksAuthMethod() {
        return socksAuthMethod;
    }

    public void setSocksAuthMethod(Byte socksAuthMethod) {
        this.socksAuthMethod = socksAuthMethod;
    }

    public Byte getCommand() {
        return command;
    }

    public void setCommand(Byte command) {
        this.command = command;
    }

    public Byte getAddressType() {
        return addressType;
    }

    public void setAddressType(Byte addressType) {
        this.addressType = addressType;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getPort() {
        return port;
    }

    public void setPort(byte[] port) {
        this.port = port;
    }
}
