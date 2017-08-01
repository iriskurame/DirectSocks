package github.yukinomiu.directsocks.common.crypto;

import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;

import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;

/**
 * Yukinomiu
 * 2017/7/31
 */
public interface Crypto {
    int getWriteBufferSizeDelta();

    void encrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException;

    void decrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException;
}
