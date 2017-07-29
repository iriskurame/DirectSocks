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
public class CachedMD5TokenConverter implements TokenConverter {
    private static final Logger logger = LoggerFactory.getLogger(CachedMD5TokenConverter.class);
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
    public byte[] convertToken(String key) {
        Context context = CONTEXT_THREAD_LOCAL.get();
        if (context == null) {
            context = new Context();
            CONTEXT_THREAD_LOCAL.set(context);
        }

        return context.convertToken(key);
    }

    private static final class Context implements TokenConverter {
        private final MessageDigest messageDigest;
        private final int targetLength;

        private String lastKey;
        private byte[] lastDigest;

        private Context() {
            try {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logger.error("初始化异常", e);
                throw new TokenConverterRuntimeException("初始化异常", e);
            }

            targetLength = messageDigest.getDigestLength();

            lastKey = String.valueOf(System.currentTimeMillis());
            lastDigest = messageDigest.digest(lastKey.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public int targetLength() {
            return targetLength;
        }

        @Override
        public byte[] convertToken(final String key) {
            if (lastKey.equals(key)) {
                return lastDigest;
            }

            messageDigest.reset();

            lastKey = key;
            lastDigest = messageDigest.digest(lastKey.getBytes(StandardCharsets.UTF_8));
            return lastDigest;
        }
    }
}
