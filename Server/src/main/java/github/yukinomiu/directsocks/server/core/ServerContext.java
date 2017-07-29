package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.server.exception.ServerRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerContext implements CloseableAttachment {
    private static final Logger logger = LoggerFactory.getLogger(ServerContext.class);

    private final ServerChannelRole serverChannelRole;

    private final CloseableAttachment currentChannelContext;
    private CubeContext associatedCubeContext;

    private boolean closeFlag = false;

    ServerContext(final ServerChannelRole serverChannelRole) {
        this.serverChannelRole = serverChannelRole;

        switch (this.serverChannelRole) {
            case CLIENT_ROLE:
                currentChannelContext = new ClientRoleContext();
                break;

            case TARGET_ROLE:
                currentChannelContext = new TargetRoleContext();
                break;

            default:
                throw new ServerRuntimeException("ServerChannelRole不支持");
        }
    }

    @Override
    public void close() throws IOException {
        if (closeFlag) return;

        closeFlag = true;
        try {
            currentChannelContext.close();
        } catch (Exception e) {
            logger.error("关闭ServerContext异常", e);
        }
    }

    public ServerChannelRole getServerChannelRole() {
        return serverChannelRole;
    }

    public CloseableAttachment getCurrentChannelContext() {
        return currentChannelContext;
    }

    public CubeContext getAssociatedCubeContext() {
        return associatedCubeContext;
    }

    public void setAssociatedCubeContext(CubeContext associatedCubeContext) {
        this.associatedCubeContext = associatedCubeContext;
    }
}
