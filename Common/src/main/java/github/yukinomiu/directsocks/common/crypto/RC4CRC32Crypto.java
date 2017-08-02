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

    private static final int MAX_PADDING = EmptyPackageCrypto.MAX_PADDING + 4;
    private static final String ALGORITHM_NAME = "RC4";
    private static final ThreadLocal<Context> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    private final String secret;

    public RC4CRC32Crypto(final String secret) {
        this.secret = secret;
    }

    @Override
    public int getMaxPadding() {
        return MAX_PADDING;
    }

    @Override
    public void encrypt(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context(secret);
            CONTEXT_THREAD_LOCAL.set(context);
        }
        context.encrypt(src, dst);
    }

    @Override
    public boolean decrypt(final ByteBuffer frame, final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context(secret);
            CONTEXT_THREAD_LOCAL.set(context);
        }
        return context.decrypt(frame, src, dst);
    }

    private final class Context extends EmptyPackageCrypto {
        private final String secret;
        private final CRC32 crc32;
        private final Cipher encryptCipher;
        private final Cipher decryptCipher;
        private final SecretKeySpec key;

        @Override
        public int getMaxPadding() {
            return RC4CRC32Crypto.MAX_PADDING;
        }

        private Context(final String secret) {
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
        protected void encryptPackage(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
            // get origin data crc32
            src.mark();
            crc32.reset();
            crc32.update(src);
            final int checksum = (int) crc32.getValue();
            src.reset();

            // write crc32
            dst.putInt(checksum);

            // write encrypt data
            try {
                encryptCipher.doFinal(src, dst);
            } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                throw new CryptoException("encrypt exception: " + e.getMessage(), e);
            }
        }

        @Override
        protected void decryptPackage(final ByteBuffer src, final ByteBuffer dst) throws CryptoException {
            // get origin data crc32
            final int checksum = src.getInt();

            // decrypt data
            final int backupPosition = dst.position();
            try {
                decryptCipher.doFinal(src, dst);
            } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
                throw new CryptoException("encrypt exception: " + e.getMessage(), e);
            }

            // verify
            dst.flip();
            dst.position(backupPosition);
            crc32.reset();
            crc32.update(dst);
            final int actualChecksum = (int) crc32.getValue();

            if (actualChecksum != checksum) {
                logger.warn("checksum verify fail");
                dst.position(backupPosition);
            }
            dst.limit(dst.capacity());
        }
    }
}
