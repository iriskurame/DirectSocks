package github.yukinomiu.directsocks.common.crypto.exception;

/**
 * Yukinomiu
 * 2017/7/31
 */
public class CryptoRuntimeException extends RuntimeException {
    public CryptoRuntimeException() {
    }

    public CryptoRuntimeException(String message) {
        super(message);
    }

    public CryptoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
