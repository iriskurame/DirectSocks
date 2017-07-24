package github.yukinomiu.directsocks.client.exception;

/**
 * Yukinomiu
 * 2017/7/13
 */
public class ClientException extends Exception {
    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
