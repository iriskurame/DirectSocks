package github.yukinomiu.directsocks.common.auth;

/**
 * Yukinomiu
 * 2017/7/29
 */
public interface TokenChecker {
    boolean check(final byte[] token);

    boolean check(final String key);

    void add(final String[] keys);

    void add(final String key);

    boolean remove(final String key);

    String[] listKeys();
}
