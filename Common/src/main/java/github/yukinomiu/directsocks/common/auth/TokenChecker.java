package github.yukinomiu.directsocks.common.auth;

/**
 * Yukinomiu
 * 2017/7/29
 */
public interface TokenChecker {
    boolean check(final byte[] token);

    void add(final String[] keys);

    void add(final String key);

    void delete(final String key);
}
