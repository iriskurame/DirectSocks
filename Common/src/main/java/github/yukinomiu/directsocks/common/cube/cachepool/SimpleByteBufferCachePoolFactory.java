package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.CubeConfig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Yukinomiu
 * 2017/7/16
 */
public class SimpleByteBufferCachePoolFactory implements ByteBufferCachePoolFactory {
    private final CubeConfig cubeConfig;

    SimpleByteBufferCachePoolFactory(final CubeConfig cubeConfig) throws ByteBufferCachePoolFactoryInitException {
        checkConfig(cubeConfig);
        this.cubeConfig = cubeConfig;
    }

    @Override
    public ByteBuffer create() {
        return ByteBuffer.allocate(cubeConfig.getBufferSize())
                .order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public void refresh(ByteBuffer object) {
        if (object == null) throw new NullPointerException("ByteBuffer为空");

        object.clear();
        object.order(ByteOrder.BIG_ENDIAN);
    }

    @Override
    public boolean validate(ByteBuffer object) {
        return object != null;
    }

    @Override
    public void destroy(ByteBuffer object) {
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }

    private void checkConfig(final CubeConfig cubeConfig) throws ByteBufferCachePoolFactoryInitException {
        if (cubeConfig == null) throw new ByteBufferCachePoolFactoryInitException("配置不能为空");

        Integer bufferSize = cubeConfig.getBufferSize();
        if (bufferSize == null) throw new ByteBufferCachePoolFactoryInitException("Buffer长度不能为空");
        if (bufferSize < 1024 || bufferSize > 1024 * 512)
            throw new ByteBufferCachePoolFactoryInitException("Buffer长度必须在[22, 102400]之间取值");
    }
}
