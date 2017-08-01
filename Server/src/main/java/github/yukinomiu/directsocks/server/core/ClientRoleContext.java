package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;

/**
 * Yukinomiu
 * 2017/7/29
 */
public final class ClientRoleContext implements CloseableAttachment {
    private ServerState serverState;

    private byte[] token;
    private byte connectionType;
    private byte addressType;
    private byte[] address;
    private int port;

    @Override
    public void close() {
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }

    public byte[] getToken() {
        return token;
    }

    public void setToken(byte[] token) {
        this.token = token;
    }

    public byte getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(byte connectionType) {
        this.connectionType = connectionType;
    }

    public byte getAddressType() {
        return addressType;
    }

    public void setAddressType(byte addressType) {
        this.addressType = addressType;
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
