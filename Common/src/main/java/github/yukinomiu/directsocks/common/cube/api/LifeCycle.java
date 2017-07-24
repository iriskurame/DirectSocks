package github.yukinomiu.directsocks.common.cube.api;

/**
 * Yukinomiu
 * 2017/7/14
 */
public interface LifeCycle {
    enum State {
        NEW, STARTING, RUNNING, STOPING, STOPED
    }

    void start();

    void shutdown();
}
