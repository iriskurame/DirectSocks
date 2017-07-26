package github.yukinomiu.directsocks.client.server;

import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.client.exception.ClientStateException;
import github.yukinomiu.directsocks.common.cube.Cube;
import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.exception.CubeInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Yukinomiu
 * 2017/7/24
 */
public class Client implements LifeCycle {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    private LifeCycle.State state;

    private final ClientNioHandle clientNioHandle;
    private final Cube cube;

    public Client(final ClientConfig clientConfig) throws ClientInitException {
        state = LifeCycle.State.NEW;
        checkConfig(clientConfig);

        clientNioHandle = new ClientNioHandle(clientConfig);
        try {
            cube = new Cube(clientConfig, clientNioHandle);
        } catch (CubeInitException e) {
            logger.error("初始化客户端异常", e);
            throw new ClientInitException("初始化客户端异常", e);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new ClientStateException();
        state = State.STARTING;

        cube.start();

        state = State.RUNNING;
        logger.debug("Client成功启动");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new ClientStateException();
        state = State.STOPING;

        cube.shutdown();

        state = State.STOPED;
        logger.debug("Client成功关闭");
    }

    private void checkConfig(final ClientConfig clientConfig) throws ClientInitException {
        if (clientConfig == null) throw new ClientInitException("配置为空");
    }
}
