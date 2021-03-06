package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.server.exception.ServerRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/27
 */
public final class ServerContext implements CloseableAttachment {
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
                logger.error("ServerChannelRole not supported: {}", this.serverChannelRole);
                throw new ServerRuntimeException("ServerChannelRole not supported");
        }
    }

    @Override
    public void close() {
        if (closeFlag) return;

        closeFlag = true;
        try {
            currentChannelContext.close();
        } catch (Exception e) {
            logger.error("closing ServerContext exception", e);
        }

        if (associatedCubeContext != null && !associatedCubeContext.isClosed()) {
            try {
                associatedCubeContext.closeAfterWrite();
                associatedCubeContext.readyWrite();
            } catch (Exception e) {
                logger.error("closing associated server CubeContext exception", e);
            }
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
