package github.yukinomiu.directsocks.common.cube.cachepool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Yukinomiu
 * 2017/7/16
 */
public final class SimpleByteBufferCachePoolFactory implements ByteBufferCachePoolFactory {
    private final int BUFFER_SIZE;

    SimpleByteBufferCachePoolFactory(final int bufferSize) throws ByteBufferCachePoolFactoryInitException {
        if (bufferSize < 1) throw new ByteBufferCachePoolFactoryInitException("buffer size can not be less than 1");
        BUFFER_SIZE = bufferSize;
    }

    @Override
    public ByteBuffer create() {
        return ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void refresh(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) throw new NullPointerException("ByteBuffer can not be null");

        byteBuffer.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public boolean validate(final ByteBuffer byteBuffer) {
        return byteBuffer != null && byteBuffer.order().equals(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void destroy(final ByteBuffer byteBuffer) {
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }
}
