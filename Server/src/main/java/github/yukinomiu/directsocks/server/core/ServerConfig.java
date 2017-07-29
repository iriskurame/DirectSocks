package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.auth.TokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.common.cube.CubeConfig;

/**
 * Yukinomiu
 * 2017/7/27
 */
public class ServerConfig extends CubeConfig {
    private TokenConverter tokenConverter;
    private TokenChecker tokenChecker;

    public TokenConverter getTokenConverter() {
        return tokenConverter;
    }

    public void setTokenConverter(TokenConverter tokenConverter) {
        this.tokenConverter = tokenConverter;
    }

    public TokenChecker getTokenChecker() {
        return tokenChecker;
    }

    public void setTokenChecker(TokenChecker tokenChecker) {
        this.tokenChecker = tokenChecker;
    }
}
