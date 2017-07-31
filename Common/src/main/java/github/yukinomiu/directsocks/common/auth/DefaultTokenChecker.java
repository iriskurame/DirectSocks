package github.yukinomiu.directsocks.common.auth;

import github.yukinomiu.directsocks.common.auth.exception.TokenCheckerRuntimeException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Yukinomiu
 * 2017/7/29
 */
public class DefaultTokenChecker implements TokenChecker {
    private final Set<Integer> tokenSet = new HashSet<>();
    private final Set<String> keySet = new HashSet<>();
    private final TokenConverter tokenConverter;

    public DefaultTokenChecker(final TokenConverter tokenConverter) {
        if (tokenConverter == null) throw new TokenCheckerRuntimeException("TokenConverter can not be null");
        this.tokenConverter = tokenConverter;
    }

    @Override
    public boolean check(final byte[] token) {
        int hashCode = Arrays.hashCode(token);
        return tokenSet.contains(hashCode);
    }

    @Override
    public boolean check(final String key) {
        return keySet.contains(key);
    }

    @Override
    public void add(final String[] keys) {
        synchronized (tokenSet) {
            for (String key : keys) {
                keySet.add(key);
                byte[] token = tokenConverter.convertToken(key);
                int hashCode = Arrays.hashCode(token);
                tokenSet.add(hashCode);
            }
        }
    }

    @Override
    public void add(final String key) {
        synchronized (tokenSet) {
            keySet.add(key);
            byte[] token = tokenConverter.convertToken(key);
            int hashCode = Arrays.hashCode(token);
            tokenSet.add(hashCode);
        }
    }

    @Override
    public boolean remove(final String key) {
        synchronized (tokenSet) {
            keySet.remove(key);
            byte[] token = tokenConverter.convertToken(key);
            int hashCode = Arrays.hashCode(token);
            return tokenSet.remove(hashCode);
        }
    }

    @Override
    public String[] listKeys() {
        String[] keys = new String[keySet.size()];
        return keySet.toArray(keys);
    }
}
