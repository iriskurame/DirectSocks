package github.yukinomiu.directsocks.common.cube.api;

import github.yukinomiu.directsocks.common.cube.CubeContext;

/**
 * Yukinomiu
 * 2017/7/20
 */
public interface NioHandle {
    void handleRead(final CubeContext cubeContext);
}
