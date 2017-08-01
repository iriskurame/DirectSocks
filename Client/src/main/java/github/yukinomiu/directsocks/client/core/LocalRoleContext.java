package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;

/**
 * Yukinomiu
 * 2017/7/28
 */
public final class LocalRoleContext implements CloseableAttachment {
    private LocalState localState;

    private Byte authMethod;
    private Byte command;
    private Byte addressType;
    private byte[] address;
    private byte[] port;

    @Override
    public void close() {
    }

    public LocalState getLocalState() {
        return localState;
    }

    public void setLocalState(LocalState localState) {
        this.localState = localState;
    }

    public Byte getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(Byte authMethod) {
        this.authMethod = authMethod;
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
