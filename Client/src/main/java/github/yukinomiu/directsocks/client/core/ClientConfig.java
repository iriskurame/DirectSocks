package github.yukinomiu.directsocks.client.core;

import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.common.cube.CubeConfig;

import java.net.InetAddress;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientConfig extends CubeConfig {
    private Boolean localDnsResolve;
    private InetAddress serverAddress;
    private Integer serverPort;
    private TokenConverter tokenConverter;
    private String key;

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

    public TokenConverter getTokenConverter() {
        return tokenConverter;
    }

    public void setTokenConverter(TokenConverter tokenConverter) {
        this.tokenConverter = tokenConverter;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
