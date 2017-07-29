package github.yukinomiu.directsocks.common.auth.exception;

/**
 * Yukinomiu
 * 2017/7/29
 */
public class TokenCheckerRuntimeException extends RuntimeException {
    public TokenCheckerRuntimeException() {
    }

    public TokenCheckerRuntimeException(String message) {
        super(message);
    }

    public TokenCheckerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
