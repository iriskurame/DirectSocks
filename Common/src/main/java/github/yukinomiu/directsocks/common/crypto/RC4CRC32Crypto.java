package github.yukinomiu.directsocks.common.crypto;

import github.yukinomiu.directsocks.common.crypto.exception.CryptoException;
import github.yukinomiu.directsocks.common.crypto.exception.CryptoRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Yukinomiu
 * 2017/7/31
 */
public final class RC4CRC32Crypto implements Crypto {
    private static final Logger logger = LoggerFactory.getLogger(RC4CRC32Crypto.class);

    private static final String ALGORITHM_NAME = "RC4";
    private static final int WRITE_BUFFER_SIZE_DELTA = 4;
    private static final ThreadLocal<Context> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    private final String secret;

    public RC4CRC32Crypto(final String secret) {
        this.secret = secret;
    }

    @Override
    public int getWriteBufferSizeDelta() {
        return WRITE_BUFFER_SIZE_DELTA;
    }

    @Override
    public void encrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context(secret);
            CONTEXT_THREAD_LOCAL.set(context);
        }
        context.encrypt(src, dst);
    }

    @Override
    public void decrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context(secret);
            CONTEXT_THREAD_LOCAL.set(context);
        }
        context.decrypt(src, dst);
    }

    private static class Context implements Crypto {
        private final String secret;
        private final CRC32 crc32;
        private final Cipher encryptCipher;
        private final Cipher decryptCipher;
        private final SecretKeySpec key;

        @Override
        public int getWriteBufferSizeDelta() {
            return WRITE_BUFFER_SIZE_DELTA;
        }

        public Context(final String secret) {
            this.secret = secret;
            crc32 = new CRC32();

            try {
                encryptCipher = Cipher.getInstance(ALGORITHM_NAME);
                decryptCipher = Cipher.getInstance(ALGORITHM_NAME);
            } catch (Exception e) {
                throw new CryptoRuntimeException("getReadCache crypto exception", e);
            }

            key = new SecretKeySpec(this.secret.getBytes(StandardCharsets.UTF_8), ALGORITHM_NAME);

            // init cipher
            try {
                encryptCipher.init(Cipher.ENCRYPT_MODE, key);
                decryptCipher.init(Cipher.DECRYPT_MODE, key);
            } catch (Exception e) {
                throw new CryptoRuntimeException("init crypto exception", e);
            }
        }

        @Override
        public void encrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException {
            /*// write crc32
            crc32.reset();
            crc32.update(src);
            src.position(0);
            int checksum = (int) crc32.getValue();
            dst.putInt(checksum);*/

            // rc4 encrypt
            try {
                encryptCipher.doFinal(src, dst);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new CryptoException("encrypt exception", e);
            }
            dst.put(src);
        }

        @Override
        public void decrypt(final ByteBuffer src, final ByteBuffer dst) throws ShortBufferException, CryptoException {
            // int checksum = src.getInt();
            try {
                decryptCipher.doFinal(src, dst);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new CryptoException("decrypt exception", e);
            }

            /*// verify checksum
            dst.flip();
            crc32.reset();
            crc32.update(dst);
            int actualChecksum = (int) crc32.getValue();
            dst.limit(dst.capacity());

            if (checksum != actualChecksum) {
                logger.warn("CRC32 checksum fail");
            }
            else {
                System.out.println("crc32 success");
            }
            dst.put(src);*/
        }
    }
}
