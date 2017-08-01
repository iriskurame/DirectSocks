package github.yukinomiu.directsocks.common.crypto;

import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;

import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;

/**
 * Yukinomiu
 * 2017/7/31
 */
public final class EmptyCrypto implements Crypto {
    public EmptyCrypto(final String secret) {
    }

    @Override
    public int getWriteBufferSizeDelta() {
        return 0;
    }

    @Override
    public void encrypt(ByteBuffer src, ByteBuffer dst) throws ShortBufferException, CryptoException {
        dst.put(src);
    }

    @Override
    public void decrypt(ByteBuffer src, ByteBuffer dst) throws ShortBufferException, CryptoException {
        dst.put(src);
    }
}
