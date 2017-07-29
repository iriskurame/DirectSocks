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
    private final Set<Integer> set = new HashSet<>();
    private final TokenConverter tokenConverter;

    public DefaultTokenChecker(final TokenConverter tokenConverter) {
        if (tokenConverter == null) throw new TokenCheckerRuntimeException("TokenConverter不能为空");
        this.tokenConverter = tokenConverter;
    }

    @Override
    public boolean check(final byte[] token) {
        int hashCode = Arrays.hashCode(token);
        return set.contains(hashCode);
    }

    @Override
    public void add(final String[] keys) {
        synchronized (set) {
            for (String key : keys) {
                byte[] token = tokenConverter.convertToken(key);
                int hashCode = Arrays.hashCode(token);
                set.add(hashCode);
            }
        }
    }

    @Override
    public void add(final String key) {
        synchronized (set) {
            byte[] token = tokenConverter.convertToken(key);
            int hashCode = Arrays.hashCode(token);
            set.add(hashCode);
        }
    }

    @Override
    public void delete(final String key) {
        synchronized (set) {
            byte[] token = tokenConverter.convertToken(key);
            int hashCode = Arrays.hashCode(token);
            set.remove(hashCode);
        }
    }
}
