package github.yukinomiu.directsocks.common.crypto;

import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;

import java.nio.ByteBuffer;

/**
 * Yukinomiu
 * 2017/7/31
 */
public class EmptyPackageCrypto implements Crypto {
    static final int MAX_PADDING = 2;

    @Override
    public int getMaxPadding() {
        return MAX_PADDING;
    }

    @Override
    public void encrypt(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        final int firstPosition = dst.position();
        final int writePosition = firstPosition + 2;
        dst.position(writePosition);
        encryptPackage(src, dst);
        final int backupPosition = dst.position();
        final short length = (short) (backupPosition - writePosition);
        dst.position(firstPosition);
        dst.putShort(length);
        dst.position(backupPosition);
    }

    @Override
    public boolean decrypt(final ByteBuffer frame, final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        if (frame.position() == 0) {
            // without context
            if (src.remaining() >= 2) {
                final short length = src.getShort();
                if (src.remaining() == length) {
                    decryptPackage(src, dst);
                    return true;
                } else if (src.remaining() < length) {
                    src.position(src.position() - 2);
                    frame.put(src);
                    return false;
                } else {
                    final int backupLimit = src.limit();
                    src.limit(src.position() + length);
                    decryptPackage(src, dst);
                    src.limit(backupLimit);
                    while (decrypt(frame, src, dst)) {
                    }
                    return true;
                }
            } else {
                frame.put(src);
                return false;
            }
        } else if (frame.position() >= 2) {
            // with context
            frame.flip();
            final short length = frame.getShort();
            final int needToRead = length - frame.remaining();

            frame.position(frame.limit());
            frame.limit(frame.capacity());
            if (src.remaining() == needToRead) {
                frame.put(src);
                frame.flip();
                frame.position(2);
                decryptPackage(frame, dst);
                frame.clear();
                return true;
            } else if (src.remaining() < needToRead) {
                frame.put(src);
                return false;
            } else {
                final int backupLimit = src.limit();
                src.limit(src.position() + needToRead);
                frame.put(src);
                src.limit(backupLimit);

                frame.flip();
                frame.position(2);
                decryptPackage(frame, dst);
                frame.clear();
                while (decrypt(frame, src, dst)) {
                }
                return true;
            }

        } else {
            // frame has 1 byte, frame position = 1
            if (src.remaining() >= 1) {
                frame.put(src.get());
                frame.flip();
                final short length = frame.getShort();

                if (src.remaining() == length) {
                    decryptPackage(src, dst);
                    frame.clear();
                    return true;
                } else if (src.remaining() < length) {
                    frame.limit(frame.capacity());
                    frame.put(src);
                    return false;
                } else {
                    final int backupLimit = src.limit();
                    src.limit(src.position() + length);
                    decryptPackage(src, dst);
                    src.limit(backupLimit);
                    frame.clear();
                    while (decrypt(frame, src, dst)) {
                    }
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    protected void encryptPackage(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        dst.put(src);
    }

    protected void decryptPackage(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        dst.put(src);
    }
}
