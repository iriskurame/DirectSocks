package github.yukinomiu.directsocks.common.cube.exception;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class CubeStateException extends CubeRuntimeException {
    public CubeStateException() {
    }

    public CubeStateException(String message) {
        super(message);
    }

    public CubeStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
