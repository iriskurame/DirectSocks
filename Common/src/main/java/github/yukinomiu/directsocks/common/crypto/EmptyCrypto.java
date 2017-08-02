package github.yukinomiu.directsocks.common.crypto;

import java.nio.ByteBuffer;

/**
 * Yukinomiu
 * 2017/7/31
 */
public final class EmptyCrypto implements Crypto {
    @Override
    public int getMaxPadding() {
        return 0;
    }

    @Override
    public void encrypt(final ByteBuffer src, final ByteBuffer dst) {
        dst.put(src);
    }

    @Override
    public boolean decrypt(final ByteBuffer frame, final ByteBuffer src, final ByteBuffer dst) {
        dst.put(src);
        return true;
    }
}
