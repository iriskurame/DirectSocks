package github.yukinomiu.directsocks.common.cube.exception;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class CubeRuntimeException extends RuntimeException {
    public CubeRuntimeException() {
    }

    public CubeRuntimeException(String message) {
        super(message);
    }

    public CubeRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
