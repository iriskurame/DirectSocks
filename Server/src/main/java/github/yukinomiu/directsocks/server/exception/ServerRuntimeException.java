package github.yukinomiu.directsocks.server.exception;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerRuntimeException extends RuntimeException {
    public ServerRuntimeException() {
    }

    public ServerRuntimeException(String message) {
        super(message);
    }

    public ServerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
