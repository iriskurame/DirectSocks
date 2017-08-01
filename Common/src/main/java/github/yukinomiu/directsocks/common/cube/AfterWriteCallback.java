package github.yukinomiu.directsocks.common.cube;

/**
 * Yukinomiu
 * 2017/8/1
 */
@FunctionalInterface
public interface AfterWriteCallback {
    void doCallback(Object arg);
}
