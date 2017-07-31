package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.CubeConfig;
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

    private final ByteBufferCachePoolFactory factory;
    private final ReentrantLock lock = new ReentrantLock();
    private final ByteBuffer[] stack;
    private final int maxIndex;
    private int top;

    private long getCount;
    private long returnCount;

    public SimpleByteBufferCachePool(final CubeConfig cubeConfig) throws ByteBufferCachePoolInitException {
        checkConfig(cubeConfig);

        factory = new SimpleByteBufferCachePoolFactory(cubeConfig);

        stack = new ByteBuffer[cubeConfig.getPoolSize()];
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
                logger.info("cache pool out of stock");
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
        logger.debug("{}: getCount={}, returnCount={}", this, getCount, returnCount);
    }

    private void checkConfig(final CubeConfig cubeConfig) throws ByteBufferCachePoolInitException {
        if (cubeConfig == null) throw new ByteBufferCachePoolInitException("config can not be null");

        Integer poolSize = cubeConfig.getPoolSize();
        if (poolSize == null) throw new ByteBufferCachePoolInitException("pool size can not be null");
        if (poolSize < 1) throw new ByteBufferCachePoolInitException("pool size must greater than 1");
    }
}
