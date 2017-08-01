package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.common.auth.TokenGenerator;
import github.yukinomiu.directsocks.common.crypto.Crypto;
import github.yukinomiu.directsocks.common.cube.CubeConfig;

import java.net.InetAddress;

/**
 * Yukinomiu
 * 2017/7/24
 */
public final class ClientConfig extends CubeConfig {
    private Boolean localDnsResolve;
    private InetAddress serverAddress;
    private Integer serverPort;
    private TokenGenerator tokenGenerator;
    private String key;
    private String secret;
    private Crypto crypto;

    public Boolean getLocalDnsResolve() {
        return localDnsResolve;
    }

    public void setLocalDnsResolve(Boolean localDnsResolve) {
        this.localDnsResolve = localDnsResolve;
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(InetAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public TokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }

    public void setTokenGenerator(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
