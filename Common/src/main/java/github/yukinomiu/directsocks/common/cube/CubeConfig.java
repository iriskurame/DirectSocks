package github.yukinomiu.directsocks.common.cube;

import java.net.InetAddress;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class CubeConfig {
    // dispatcher config
    private InetAddress bindAddress;
    private Integer bindPort;
    private Integer backlog;

    // docker config
    private Integer workerCount;

    // switcher config
    private Boolean tcpNoDelay;
    private Boolean tcpKeepAlive;

    // buffer config
    private Integer readBufferSize;
    private Integer readPoolSize;
    private Integer writeBufferSize;
    private Integer writePoolSize;
    private Integer frameBufferSize;
    private Integer framePoolSize;

    public InetAddress getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(InetAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Integer getBindPort() {
        return bindPort;
    }

    public void setBindPort(Integer bindPort) {
        this.bindPort = bindPort;
    }

    public Integer getBacklog() {
        return backlog;
    }

    public void setBacklog(Integer backlog) {
        this.backlog = backlog;
    }

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public Boolean getTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(Boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public Boolean getTcpKeepAlive() {
        return tcpKeepAlive;
    }

    public void setTcpKeepAlive(Boolean tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }

    public Integer getReadBufferSize() {
        return readBufferSize;
    }

    public void setReadBufferSize(Integer readBufferSize) {
        this.readBufferSize = readBufferSize;
    }

    public Integer getReadPoolSize() {
        return readPoolSize;
    }

    public void setReadPoolSize(Integer readPoolSize) {
        this.readPoolSize = readPoolSize;
    }

    public Integer getWriteBufferSize() {
        return writeBufferSize;
    }

    public void setWriteBufferSize(Integer writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    public Integer getWritePoolSize() {
        return writePoolSize;
    }

    public void setWritePoolSize(Integer writePoolSize) {
        this.writePoolSize = writePoolSize;
    }

    public Integer getFrameBufferSize() {
        return frameBufferSize;
    }

    public void setFrameBufferSize(Integer frameBufferSize) {
        this.frameBufferSize = frameBufferSize;
    }

    public Integer getFramePoolSize() {
        return framePoolSize;
    }

    public void setFramePoolSize(Integer framePoolSize) {
        this.framePoolSize = framePoolSize;
    }
}
