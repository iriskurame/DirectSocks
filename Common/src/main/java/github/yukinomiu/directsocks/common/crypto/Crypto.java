package github.yukinomiu.directsocks.common.crypto;

import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;

import java.nio.ByteBuffer;

/**
 * Yukinomiu
 * 2017/7/31
 */
public interface Crypto {
    int getMaxPadding();

    void encrypt(final ByteBuffer src, final ByteBuffer dst) throws CryptoException;

    boolean decrypt(final ByteBuffer frame, final ByteBuffer src, final ByteBuffer dst) throws CryptoException;
}
