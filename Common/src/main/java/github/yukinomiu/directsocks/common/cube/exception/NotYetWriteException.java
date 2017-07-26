package github.yukinomiu.directsocks.common.cube.exception;

/**
 * Yukinomiu
 * 2017/7/26
 */
public class NotYetWriteException extends CubeException {
    public NotYetWriteException() {
    }

    public NotYetWriteException(String message) {
        super(message);
    }

    public NotYetWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
