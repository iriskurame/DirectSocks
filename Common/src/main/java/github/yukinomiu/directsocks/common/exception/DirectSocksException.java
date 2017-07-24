package github.yukinomiu.directsocks.common.exception;

/**
 * Yukinomiu
 * 2017/7/25
 */
public class DirectSocksException extends Exception {
    public DirectSocksException() {
    }

    public DirectSocksException(String message) {
        super(message);
    }

    public DirectSocksException(String message, Throwable cause) {
        super(message, cause);
    }
}
