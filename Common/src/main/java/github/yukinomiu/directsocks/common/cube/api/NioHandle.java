package github.yukinomiu.directsocks.common.cube.api;

import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;

/**
 * Yukinomiu
 * 2017/7/20
 */
public interface NioHandle {
    void handleRead(final CubeContext cubeContext);

    void handleConnectedSuccess(final CubeContext cubeContext);

    void handleConnectedFail(final CubeContext cubeContext, final CubeConnectionException cubeConnectionException);
}
