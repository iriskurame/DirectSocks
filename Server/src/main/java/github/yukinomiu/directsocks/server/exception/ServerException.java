package github.yukinomiu.directsocks.server.exception;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerException extends Exception {
    public ServerException() {
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
