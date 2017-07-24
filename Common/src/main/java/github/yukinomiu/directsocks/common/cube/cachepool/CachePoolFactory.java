package github.yukinomiu.directsocks.common.cube.cachepool;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;

/**
 * Yukinomiu
 * 2017/7/16
 */
public interface CachePoolFactory<T> extends LifeCycle {
    T create();

    void refresh(T object);

    boolean validate(T object);

    void destroy(T object);
}
