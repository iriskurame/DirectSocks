package github.yukinomiu.directsocks.common.auth;

import github.yukinomiu.directsocks.common.auth.exception.TokenConverterRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Yukinomiu
 * 2017/7/29
 */
public final class CachedMD5TokenGenerator implements TokenGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CachedMD5TokenGenerator.class);
    private static final ThreadLocal<Context> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public int targetLength() {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context();
            CONTEXT_THREAD_LOCAL.set(context);
        }

        return context.targetLength();
    }

    @Override
    public byte[] generate(String key) {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context();
            CONTEXT_THREAD_LOCAL.set(context);
        }

        return context.generate(key);
    }

    private static final class Context implements TokenGenerator {
        private final MessageDigest messageDigest;
        private final int targetLength;

        private String lastKey;
        private byte[] lastToken;

        private Context() {
            try {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logger.error("init exception", e);
                throw new TokenConverterRuntimeException("init exception", e);
            }

            targetLength = messageDigest.getDigestLength();

            lastKey = String.valueOf(System.currentTimeMillis());
            lastToken = messageDigest.digest(lastKey.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int targetLength() {
            return targetLength;
        }

        @Override
        public byte[] generate(final String key) {
            if (lastKey.equals(key)) {
                return lastToken;
            }

            messageDigest.reset();

            lastKey = key;
            lastToken = messageDigest.digest(lastKey.getBytes(StandardCharsets.UTF_8));
            return lastToken;
        }
    }
}
