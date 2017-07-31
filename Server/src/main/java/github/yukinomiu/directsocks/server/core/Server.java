package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.cube.Cube;
import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.exception.CubeInitException;
import github.yukinomiu.directsocks.server.exception.ServerInitException;
import github.yukinomiu.directsocks.server.exception.ServerStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class Server implements LifeCycle {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private LifeCycle.State state;

    private final Cube cube;

    public Server(final ServerConfig serverConfig) throws ServerInitException {
        state = LifeCycle.State.NEW;
        checkConfig(serverConfig);

        NioHandle serverNioHandle = new ServerNioHandle(serverConfig);
        try {
            cube = new Cube(serverConfig, serverNioHandle);
        } catch (CubeInitException e) {
            logger.error("init Server exception", e);
            throw new ServerInitException("init Server exception", e);
        }
    }

    @Override
    public void start() {
        if (state != State.NEW) throw new ServerStateException();
        state = State.STARTING;

        cube.start();

        state = State.RUNNING;
        logger.debug("Server started");
    }

    @Override
    public void shutdown() {
        if (state != State.RUNNING) throw new ServerStateException();
        state = State.STOPPING;

        cube.shutdown();

        state = State.STOPPED;
        logger.debug("Server closed");
    }

    private void checkConfig(final ServerConfig serverConfig) throws ServerInitException {
        if (serverConfig == null) throw new ServerInitException("config can not be null");
    }
}
