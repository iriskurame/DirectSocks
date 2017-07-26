package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.exception.CubeInitException;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class Cube implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Cube.class);

    private LifeCycle.State state;

    private final Dispatcher dispatcher;
    private final Docker docker;

    public Cube(final CubeConfig cubeConfig, final NioHandle nioHandle) throws CubeInitException {
        state = LifeCycle.State.NEW;

        docker = new Docker(cubeConfig, nioHandle);
        dispatcher = new Dispatcher(cubeConfig, docker);
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        docker.start();
        dispatcher.start();

        state = State.RUNNING;
        logger.debug("Cube成功启动");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPING;

        dispatcher.shutdown();
        docker.shutdown();

        state = State.STOPED;
        logger.debug("Cube成功关闭");
    }
}
