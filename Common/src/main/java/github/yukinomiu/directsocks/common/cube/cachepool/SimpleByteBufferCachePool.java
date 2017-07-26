package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.CubeConfig;
import github.yukinomiu.directsocks.common.cube.exception.ByteBufferCachePoolInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Yukinomiu
 * 2017/7/16
 */
public class SimpleByteBufferCachePool implements ByteBufferCachePool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleByteBufferCachePool.class);

    private final CubeConfig cubeConfig;
    private final ByteBufferCachePoolFactory factory;
    private final ReentrantLock lock = new ReentrantLock();
    private final ByteBuffer[] stack;
    private final int maxIndex;
    private int top;

    private long getCount;
    private long returnCount;

    public SimpleByteBufferCachePool(final CubeConfig cubeConfig) throws ByteBufferCachePoolInitException {
        checkConfig(cubeConfig);
        this.cubeConfig = cubeConfig;

        factory = new SimpleByteBufferCachePoolFactory(this.cubeConfig);

        stack = new ByteBuffer[this.cubeConfig.getPoolSize()];
        for (int i = 0; i < stack.length; i++) {
            stack[i] = factory.create();
        }
        maxIndex = stack.length - 1;
        top = maxIndex;
    }

    @Override
    public ByteBuffer get() {
        lock.lock();
        getCount++;
        try {
            // pop
            if (top >= 0) {
                ByteBuffer byteBuffer = stack[top];
                stack[top] = null;
                top--;
                return byteBuffer;
            } else {
                logger.info("缓存池容量不足");
                return factory.create();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void returnBack(ByteBuffer object) {
        if (object == null) return;

        lock.lock();
        returnCount++;
        try {
            int next = top + 1;
            if (next <= maxIndex) {
                factory.refresh(object);
                stack[next] = object;
                top = next;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refresh(ByteBuffer object) {
        if (object == null) return;
        factory.refresh(object);
    }

    @Override
    public int capacity() {
        return stack.length;
    }

    @Override
    public int left() {
        lock.lock();
        try {
            return stack.length - top;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void start() {
        factory.start();
    }

    @Override
    public void shutdown() {
        factory.shutdown();
        logger.debug("getCount={}, returnCount={}", getCount, returnCount);
    }

    private void checkConfig(final CubeConfig cubeConfig) throws ByteBufferCachePoolInitException {
        if (cubeConfig == null) throw new ByteBufferCachePoolInitException("配置不能为空");

        Integer poolSize = cubeConfig.getPoolSize();
        if (poolSize == null) throw new ByteBufferCachePoolInitException("缓存池长度不能为空");
        if (poolSize < 1) throw new ByteBufferCachePoolInitException("缓存池长度必须大于1");
    }
}
