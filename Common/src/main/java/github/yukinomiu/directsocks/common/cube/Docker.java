package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.cachepool.SimpleByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import github.yukinomiu.directsocks.common.cube.exception.DokerInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

/**
 * Yukinomiu
 * 2017/7/19
 */
public final class Docker implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Docker.class);

    private LifeCycle.State state;

    private final ByteBufferCachePool readPool;
    private final ByteBufferCachePool writePool;
    private final ByteBufferCachePool framePool;

    private final int workerCount;
    private final Switcher[] switchers;

    private int next = 0;

    Docker(final CubeConfig cubeConfig, final NioHandle nioHandle) throws DokerInitException {
        state = LifeCycle.State.NEW;
        checkConfig(cubeConfig);

        readPool = new SimpleByteBufferCachePool(cubeConfig.getReadBufferSize(), cubeConfig.getReadPoolSize());
        writePool = new SimpleByteBufferCachePool(cubeConfig.getWriteBufferSize(), cubeConfig.getWritePoolSize());
        framePool = new SimpleByteBufferCachePool(cubeConfig.getFrameBufferSize(), cubeConfig.getFramePoolSize());

        workerCount = cubeConfig.getWorkerCount();
        switchers = new Switcher[workerCount];
        for (int i = 0; i < workerCount; i++) {
            switchers[i] = new Switcher(cubeConfig, nioHandle, readPool, writePool, framePool);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startDocker();

        state = State.RUNNING;
        logger.debug("Docker started");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownDocker();

        state = State.STOPPED;
        logger.debug("Docker closed");
    }

    private void checkConfig(final CubeConfig cubeConfig) throws DokerInitException {
        if (cubeConfig == null) throw new DokerInitException("config can not be null");

        if (cubeConfig.getReadBufferSize() == null) throw new DokerInitException("read buffer size can not be null");
        if (cubeConfig.getReadBufferSize() < 1024 || cubeConfig.getReadBufferSize() > 65538)
            throw new DokerInitException("read buffer size must in range 1024-65538");

        if (cubeConfig.getReadPoolSize() == null) throw new DokerInitException("read pool size can not be null");
        if (cubeConfig.getReadPoolSize() < 8) throw new DokerInitException("read pool size can not be less than 8");

        if (cubeConfig.getWriteBufferSize() == null) throw new DokerInitException("write buffer size can not be null");
        if (cubeConfig.getWriteBufferSize() < 1024 || cubeConfig.getWriteBufferSize() > 65538)
            throw new DokerInitException("write buffer size must in range 1024-65538");

        if (cubeConfig.getWritePoolSize() == null) throw new DokerInitException("write pool size can not be null");
        if (cubeConfig.getWritePoolSize() < 8) throw new DokerInitException("write pool size can not be less than 8");

        if (cubeConfig.getFrameBufferSize() == null) throw new DokerInitException("frame buffer size can not be null");
        if (cubeConfig.getFrameBufferSize() < 1024 || cubeConfig.getFrameBufferSize() > 65538)
            throw new DokerInitException("frame buffer size must in range 1024-65538");

        if (cubeConfig.getFramePoolSize() == null) throw new DokerInitException("frame pool size can not be null");
        if (cubeConfig.getFramePoolSize() < 8) throw new DokerInitException("frame pool size can not be less than 8");

        Integer workerCount = cubeConfig.getWorkerCount();
        if (workerCount == null) throw new DokerInitException("worker count can not be null");
        if (workerCount < 1 || workerCount > 16) throw new DokerInitException("worker count must in range 1-16");
    }

    private void startDocker() {
        readPool.start();
        writePool.start();
        framePool.start();

        for (Switcher switcher : switchers) {
            switcher.start();
        }
    }

    private void shutdownDocker() {
        for (Switcher switcher : switchers) {
            switcher.shutdown();
        }

        readPool.shutdown();
        writePool.shutdown();
        framePool.shutdown();
    }

    private Switcher selectSwitcher() {
        if (next == workerCount) {
            next = 0;
        }
        return switchers[next++];
    }

    void accept(final SocketChannel socketChannel) {
        Switcher targetSwitcher = selectSwitcher();
        targetSwitcher.accept(socketChannel);
    }
}
