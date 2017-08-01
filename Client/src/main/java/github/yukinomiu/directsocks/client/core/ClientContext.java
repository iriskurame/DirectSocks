package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.client.exception.ClientRuntimeException;
import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/24
 */
public final class ClientContext implements CloseableAttachment {
    private static final Logger logger = LoggerFactory.getLogger(ClientContext.class);

    private final ClientChannelRole clientChannelRole;
    private final CloseableAttachment currentChannelContext;

    private CubeContext associatedCubeContext;

    private boolean closeFlag = false;

    ClientContext(final ClientChannelRole clientChannelRole) {
        this.clientChannelRole = clientChannelRole;

        switch (this.clientChannelRole) {
            case LOCAL_ROLE:
                currentChannelContext = new LocalRoleContext();
                break;

            case SERVER_ROLE:
                currentChannelContext = new ServerRoleContext();
                break;

            default:
                logger.error("ClientChannelRole not supported: {}", this.clientChannelRole.name());
                throw new ClientRuntimeException("ClientChannelRole not supported");
        }
    }

    @Override
    public void close() {
        if (closeFlag) return;

        closeFlag = true;
        try {
            currentChannelContext.close();
        } catch (Exception e) {
            logger.error("closing ClientContext exception", e);
        }

        if (associatedCubeContext != null && !associatedCubeContext.isClosed()) {
            try {
                associatedCubeContext.closeAfterWrite();
                associatedCubeContext.readyWrite();
            } catch (Exception e) {
                logger.error("closing associated client CubeContext exception", e);
            }
        }
    }

    public ClientChannelRole getClientChannelRole() {
        return clientChannelRole;
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
