package github.yukinomiu.directsocks.common.exception;

/**
 * Yukinomiu
 * 2017/7/30
 */
public class DirectSocksConfigException extends RuntimeException {
    public DirectSocksConfigException() {
    }

    public DirectSocksConfigException(String message) {
        super(message);
    }

    public DirectSocksConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
