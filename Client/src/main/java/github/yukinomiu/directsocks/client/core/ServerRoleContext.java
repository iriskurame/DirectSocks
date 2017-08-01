package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;

/**
 * Yukinomiu
 * 2017/7/28
 */
public final class ServerRoleContext implements CloseableAttachment {
    private ServerState serverState;

    @Override
    public void close() {
    }

    public ServerState getServerState() {
        return serverState;
    }

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
    }
}
