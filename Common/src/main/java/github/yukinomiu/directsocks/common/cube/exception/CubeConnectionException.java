package github.yukinomiu.directsocks.common.cube.exception;

/**
 * Yukinomiu
 * 2017/7/28
 */
public class CubeConnectionException extends CubeException {
    public CubeConnectionException() {
    }

    public CubeConnectionException(String message) {
        super(message);
    }

    public CubeConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
