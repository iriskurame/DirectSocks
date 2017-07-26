package github.yukinomiu.directsocks.server.exception;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ServerStateException extends ServerRuntimeException {
    public ServerStateException() {
    }

    public ServerStateException(String message) {
        super(message);
    }

    public ServerStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
