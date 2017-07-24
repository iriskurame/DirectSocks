package github.yukinomiu.directsocks.common.cube.exception;

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
