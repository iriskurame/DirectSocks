package github.yukinomiu.directsocks.common.exception;

/**
 * Yukinomiu
 * 2017/7/25
 */
public class DirectSocksRuntimeException extends RuntimeException {
    public DirectSocksRuntimeException() {
    }

    public DirectSocksRuntimeException(String message) {
        super(message);
    }

    public DirectSocksRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
