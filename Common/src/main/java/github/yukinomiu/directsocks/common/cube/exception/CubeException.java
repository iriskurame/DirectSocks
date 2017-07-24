package github.yukinomiu.directsocks.common.cube.exception;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class CubeException extends Exception {
    public CubeException() {
    }

    public CubeException(String message) {
        super(message);
    }

    public CubeException(String message, Throwable cause) {
        super(message, cause);
    }
}
