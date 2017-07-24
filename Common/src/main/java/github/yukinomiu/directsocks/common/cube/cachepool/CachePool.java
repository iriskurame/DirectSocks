package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;

/**
 * Yukinomiu
 * 2017/7/16
 */
public interface CachePool<T> extends LifeCycle {
    T get();

    void returnBack(T object);

    void refresh(T object);

    int capacity();

    int left();
}
