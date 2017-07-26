package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.server.exception.ServerInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerNioHandle implements NioHandle {
    private static final Logger logger = LoggerFactory.getLogger(ServerNioHandle.class);

    private final ServerConfig serverConfig;

    public ServerNioHandle(final ServerConfig serverConfig) throws ServerInitException {
        checkConfig(serverConfig);
        this.serverConfig = serverConfig;
    }

    @Override
    public void handleRead(CubeContext cubeContext) {

    }

    private void checkConfig(final ServerConfig serverConfig) throws ServerInitException {
        if (serverConfig == null) throw new ServerInitException("配置不能为空");
    }
}
