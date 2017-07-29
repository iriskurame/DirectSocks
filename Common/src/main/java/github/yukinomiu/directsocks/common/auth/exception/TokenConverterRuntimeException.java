package github.yukinomiu.directsocks.common.auth.exception;

/**
 * Yukinomiu
 * 2017/7/28
 */
public class TokenConverterRuntimeException extends RuntimeException {
    public TokenConverterRuntimeException() {
    }

    public TokenConverterRuntimeException(String message) {
        super(message);
    }

    public TokenConverterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
