package github.yukinomiu.directsocks.client.exception;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientRuntimeException extends RuntimeException {
    public ClientRuntimeException() {
    }

    public ClientRuntimeException(String message) {
        super(message);
    }

    public ClientRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
