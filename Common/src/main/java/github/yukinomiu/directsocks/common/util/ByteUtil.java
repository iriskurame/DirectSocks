package github.yukinomiu.directsocks.common.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Yukinomiu
 * 2017/7/13
 */
public final class ByteUtil {
    private ByteUtil() {
    }

    public static String bytesToString(final byte[] bytes, final int srcPos, final int desPos) {
        if (bytes == null) throw new NullPointerException();
        if (srcPos < 0 || desPos < srcPos) throw new RuntimeException("illegal position");

        StringBuilder sb = new StringBuilder((desPos - srcPos + 1) << 1);

        for (int i = srcPos; i <= desPos; ++i) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    public static String byteBufferToString(final ByteBuffer byteBuffer, final Charset charset) {
        if (byteBuffer == null) throw new NullPointerException();

        ByteBuffer tmp = byteBuffer.asReadOnlyBuffer();
        byte[] b = new byte[tmp.remaining()];
        tmp.get(b, 0, b.length);

        return new String(b, charset);
    }

    public static String byteBufferToHexString(final ByteBuffer byteBuffer) {
        if (byteBuffer == null) throw new NullPointerException();

        ByteBuffer tmp = byteBuffer.asReadOnlyBuffer();
        byte[] b = new byte[tmp.remaining()];
        for (int i = 0; i < b.length; i++) {
            b[i] = tmp.get();
        }

        StringBuilder sb = new StringBuilder(b.length << 1);
        for (byte v : b) {
            sb.append(String.format("%02x", v));
        }
        return sb.toString();
    }

    public static int bytesToInt(final byte[] bytes, final int srcPos, final int desPos) {
        if (bytes == null) throw new NullPointerException();
        if (srcPos < 0 || desPos - srcPos > 4) throw new RuntimeException("array range too large");

        int n = 0;
        for (int i = srcPos; i <= desPos; ++i) {
            n <<= 8;
            n |= (bytes[i] & 0xff);
        }
        return n;
    }

    public static byte[] intToBytes(final int n) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int tmp = (n >> (8 * (3 - i))) & 0xff;
            result[i] = (byte) tmp;
        }
        return result;
    }
}
