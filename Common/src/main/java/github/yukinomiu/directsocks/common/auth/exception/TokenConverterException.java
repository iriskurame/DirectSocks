package github.yukinomiu.directsocks.common.auth.exception;

/**
 * Yukinomiu
 * 2017/7/28
 */
public class TokenConverterException extends Exception {
    public TokenConverterException() {
    }

    public TokenConverterException(String message) {
        super(message);
    }

    public TokenConverterException(String message, Throwable cause) {
        super(message, cause);
    }
}
