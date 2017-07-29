package github.yukinomiu.directsocks.common.auth;

/**
 * Yukinomiu
 * 2017/7/28
 */
public interface TokenConverter {
    int targetLength();

    byte[] convertToken(final String key);
}
