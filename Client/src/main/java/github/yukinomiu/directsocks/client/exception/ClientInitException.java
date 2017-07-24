package github.yukinomiu.directsocks.client.exception;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientInitException extends ClientException {
    public ClientInitException() {
    }

    public ClientInitException(String message) {
        super(message);
    }

    public ClientInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
