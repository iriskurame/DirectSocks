package github.yukinomiu.directsocks.common.auth;

/**
 * Yukinomiu
 * 2017/7/28
 */
public interface TokenGenerator {
    int targetLength();

    byte[] generate(final String key);
}
