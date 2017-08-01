package github.yukinomiu.directsocks.server.core;

import github.yukinomiu.directsocks.common.auth.TokenGenerator;
import github.yukinomiu.directsocks.common.auth.TokenVerifier;
import github.yukinomiu.directsocks.common.crypto.Crypto;
import github.yukinomiu.directsocks.common.cube.CubeConfig;

/**
 * Yukinomiu
 * 2017/7/27
 */
public final class ServerConfig extends CubeConfig {
    private TokenGenerator tokenGenerator;
    private TokenVerifier tokenVerifier;
    private String secret;
    private Crypto crypto;

    public TokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }

    public void setTokenGenerator(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public TokenVerifier getTokenVerifier() {
        return tokenVerifier;
    }

    public void setTokenVerifier(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }
}
