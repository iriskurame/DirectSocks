package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.exception.DokerInitException;

/**
 * Yukinomiu
 * 2017/7/20
 */
public class ByteBufferCachePoolInitException extends DokerInitException {
    public ByteBufferCachePoolInitException() {
    }

    public ByteBufferCachePoolInitException(String message) {
        super(message);
    }

    public ByteBufferCachePoolInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
