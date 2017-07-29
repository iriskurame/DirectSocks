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
public class Docker implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Docker.class);

    private LifeCycle.State state;

    private final ByteBufferCachePool byteBufferCachePool;
    private final int workerCount;
    private final Switcher[] switchers;

    private int next = 0;

    Docker(final CubeConfig cubeConfig, final NioHandle nioHandle) throws DokerInitException {
        state = LifeCycle.State.NEW;
        checkConfig(cubeConfig);

        byteBufferCachePool = new SimpleByteBufferCachePool(cubeConfig);
        workerCount = cubeConfig.getWorkerCount();
        switchers = new Switcher[workerCount];
        for (int i = 0; i < workerCount; i++) {
            switchers[i] = new Switcher(byteBufferCachePool, nioHandle);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startDocker();

        state = State.RUNNING;
        logger.debug("Docker成功开启");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownDocker();

        state = State.STOPPED;
        logger.debug("Docker成功关闭");
    }

    private void checkConfig(final CubeConfig cubeConfig) throws DokerInitException {
        if (cubeConfig == null) throw new DokerInitException("配置不能为空");

        Integer workerCount = cubeConfig.getWorkerCount();
        if (workerCount == null) throw new DokerInitException("worker数量不能为空");
        if (workerCount < 1 || workerCount > 16) throw new DokerInitException("worker数量必须在[1, 16]之间取值");
    }

    private void startDocker() {
        byteBufferCachePool.start();

        for (Switcher switcher : switchers) {
            try {
                switcher.start();
            } catch (Exception e) {
                logger.error("Switcher启动失败");
            }
        }
    }

    private void shutdownDocker() {
        for (Switcher switcher : switchers) {
            try {
                switcher.shutdown();
            } catch (Exception e) {
                logger.error("Switcher启动失败");
            }
        }

        byteBufferCachePool.shutdown();
    }

    private Switcher select() {
        if (next == workerCount) {
            next = 0;
        }
        return switchers[next++];
    }

    void accept(final SocketChannel socketChannel) {
        Switcher targetSwitcher = select();
        targetSwitcher.accept(socketChannel);
    }
}
