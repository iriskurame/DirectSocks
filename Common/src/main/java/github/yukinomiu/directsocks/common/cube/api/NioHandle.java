package github.yukinomiu.directsocks.common.cube.api;

import github.yukinomiu.directsocks.common.cube.CubeContext;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;

/**
 * Yukinomiu
 * 2017/7/20
 */
public interface NioHandle {
    void handleRead(final CubeContext cubeContext);

    void handleConnectSuccess(final CubeContext cubeContext);

    void handleConnectFail(final CubeContext cubeContext, final CubeConnectionException cubeConnectionException);
}
