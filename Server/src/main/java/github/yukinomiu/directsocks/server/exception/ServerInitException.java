package github.yukinomiu.directsocks.server.exception;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ServerInitException extends ServerException {
    public ServerInitException() {
    }

    public ServerInitException(String message) {
        super(message);
    }

    public ServerInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
