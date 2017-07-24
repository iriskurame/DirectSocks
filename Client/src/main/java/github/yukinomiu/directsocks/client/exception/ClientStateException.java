package github.yukinomiu.directsocks.client.exception;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientStateException extends ClientRuntimeException {
    public ClientStateException() {
    }

    public ClientStateException(String message) {
        super(message);
    }

    public ClientStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
