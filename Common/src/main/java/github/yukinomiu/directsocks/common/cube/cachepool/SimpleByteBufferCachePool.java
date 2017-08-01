package github.yukinomiu.directsocks.common.cube.cachepool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Yukinomiu
 * 2017/7/16
 */
public final class SimpleByteBufferCachePool implements ByteBufferCachePool {
    private static final Logger logger = LoggerFactory.getLogger(SimpleByteBufferCachePool.class);

    private final ReentrantLock lock = new ReentrantLock();

    private final ByteBufferCachePoolFactory factory;
    private final ByteBuffer[] stack;
    private final int maxIndex;

    private int top;

    private long getCount;
    private long returnCount;

    public SimpleByteBufferCachePool(final int bufferSize, final int poolSize) throws ByteBufferCachePoolInitException {
        factory = new SimpleByteBufferCachePoolFactory(bufferSize);

        if (poolSize < 1) throw new ByteBufferCachePoolInitException("pool size can not be less than 1");

        stack = new ByteBuffer[poolSize];
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
    public void returnBack(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) return;

        lock.lock();
        returnCount++;
        try {
            int next = top + 1;
            if (next <= maxIndex) {
                factory.refresh(byteBuffer);
                stack[next] = byteBuffer;
                top = next;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refresh(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) return;
        factory.refresh(byteBuffer);
    }

    @Override
    public int capacity() {
        return stack.length;
    }

    @Override
    public int left() {
        lock.lock();
        try {
            return top + 1;
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
}
