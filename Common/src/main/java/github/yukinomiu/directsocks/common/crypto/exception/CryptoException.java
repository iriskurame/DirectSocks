package github.yukinomiu.directsocks.common.crypto.exception;

/**
 * Yukinomiu
 * 2017/7/31
 */
public class CryptoException extends Exception {
    public CryptoException() {
    }

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
