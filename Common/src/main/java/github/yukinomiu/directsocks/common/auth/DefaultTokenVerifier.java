package github.yukinomiu.directsocks.common.auth;

import github.yukinomiu.directsocks.common.auth.exception.TokenCheckerRuntimeException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Yukinomiu
 * 2017/7/29
 */
public final class DefaultTokenVerifier implements TokenVerifier {
    private final Set<Integer> tokenSet = new HashSet<>();
    private final Set<String> keySet = new HashSet<>();
    private final TokenGenerator tokenGenerator;

    public DefaultTokenVerifier(final TokenGenerator tokenGenerator) {
        if (tokenGenerator == null) throw new TokenCheckerRuntimeException("TokenGenerator can not be null");
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public boolean verify(final byte[] token) {
        int hashCode = Arrays.hashCode(token);
        return tokenSet.contains(hashCode);
    }

    @Override
    public boolean verify(final String key) {
        return keySet.contains(key);
    }

    @Override
    public void add(final String[] keys) {
        synchronized (tokenSet) {
            for (String key : keys) {
                keySet.add(key);
                byte[] token = tokenGenerator.generate(key);
                int hashCode = Arrays.hashCode(token);
                tokenSet.add(hashCode);
            }
        }
    }

    @Override
    public void add(final String key) {
        synchronized (tokenSet) {
            keySet.add(key);
            byte[] token = tokenGenerator.generate(key);
            int hashCode = Arrays.hashCode(token);
            tokenSet.add(hashCode);
        }
    }

    @Override
    public boolean remove(final String key) {
        synchronized (tokenSet) {
            boolean exists = keySet.remove(key);
            if (exists) {
                byte[] token = tokenGenerator.generate(key);
                int hashCode = Arrays.hashCode(token);
                tokenSet.remove(hashCode);
            }
            return exists;
        }
    }

    @Override
    public String[] listKeys() {
        String[] keys = new String[keySet.size()];
        return keySet.toArray(keys);
    }
}
