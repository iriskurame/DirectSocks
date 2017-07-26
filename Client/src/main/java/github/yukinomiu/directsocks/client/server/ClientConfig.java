package github.yukinomiu.directsocks.client.server;

import github.yukinomiu.directsocks.common.cube.CubeConfig;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class ClientConfig extends CubeConfig {
    private Boolean localDnsResolve;

    public Boolean getLocalDnsResolve() {
        return localDnsResolve;
    }

    public void setLocalDnsResolve(Boolean localDnsResolve) {
        this.localDnsResolve = localDnsResolve;
    }
}
