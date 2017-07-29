package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.exception.CubeInitException;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import github.yukinomiu.directsocks.common.cube.exception.DispatcherInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class Dispatcher implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private LifeCycle.State state;
    private final Docker docker;

    private final Selector dispatcherSelector;
    private final ServerSocketChannel serverSocketChannel;
    private final ServerSocket serverSocket;

    private Thread dispatchThread;

    Dispatcher(final CubeConfig cubeConfig, final Docker docker) throws CubeInitException {
        state = LifeCycle.State.NEW;
        checkConfig(cubeConfig);

        this.docker = docker;

        try {
            // init selector
            dispatcherSelector = Selector.open();

            // init server socket channel
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            logger.error("Dispatcher IO异常", e);
            throw new CubeInitException("Dispatcher IO异常", e);
        }

        // bind server socket
        InetAddress bindAddress = cubeConfig.getBindAddress();
        int bindPort = cubeConfig.getBindPort();
        int backlog = cubeConfig.getBacklog();

        serverSocket = serverSocketChannel.socket();
        try {
            serverSocket.bind(new InetSocketAddress(bindAddress, bindPort), backlog);
        } catch (BindException e) {
            logger.error("本地端口绑定异常, 端口可能已被占用", e);
            throw new CubeInitException("本地端口绑定异常, 端口可能已被占用", e);
        } catch (IOException e) {
            logger.error("ServerSocket IO异常", e);
            throw new CubeInitException("ServerSocket IO异常", e);
        }

        // register channel to selector
        try {
            serverSocketChannel.register(dispatcherSelector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            logger.error("ServerSocketChannel已经关闭", e);
            throw new CubeInitException("ServerSocketChannel已经关闭", e);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startDispatcher();

        state = State.RUNNING;
        logger.debug("Dispatcher成功开启");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownDispatcher();

        state = State.STOPPED;
        logger.debug("Dispatcher成功关闭");
    }

    private void checkConfig(final CubeConfig cubeConfig) throws DispatcherInitException {
        if (cubeConfig == null) throw new DispatcherInitException("配置不能为空");

        InetAddress bindAddress = cubeConfig.getBindAddress();
        if (bindAddress == null) throw new DispatcherInitException("绑定地址不能为空");
        if (!(bindAddress instanceof Inet4Address) && !(bindAddress instanceof Inet6Address))
            throw new DispatcherInitException("端绑定地址类型不支持, 仅支持 IPv4 和 IPv6 地址");

        Integer bindPort = cubeConfig.getBindPort();
        if (bindPort == null) throw new DispatcherInitException("绑定端口不能为空");
        if (bindPort < 1 || bindPort > 65535) throw new DispatcherInitException("绑定端口非法, 端口必须在[1, 65535]之间取值");

        Integer backlog = cubeConfig.getBacklog();
        if (backlog == null) throw new DispatcherInitException("Backlog不能为空");
    }

    private void startDispatcher() {
        dispatchThread = new Thread(() -> {
            logger.debug("开始监听本地连接, 监听地址 {}:{}", serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());

            while (true) {
                try {
                    int selectCount = dispatcherSelector.select();

                    if (Thread.currentThread().isInterrupted()) {
                        dispatcherSelector.close();
                        break;
                    }

                    if (selectCount == 0) {
                        continue;
                    }

                    Set<SelectionKey> sets = dispatcherSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = sets.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();

                        if (!selectionKey.isValid()) {
                            continue;
                        }

                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        }
                    }

                } catch (CancelledKeyException e) {
                    logger.warn("连接已断开");
                } catch (IOException e) {
                    logger.error("Dispatcher IO异常", e);
                } catch (Exception e) {
                    logger.debug("Dispatcher异常", e);
                    break;
                }
            }

            logger.debug("停止监听本地连接");
        });

        dispatchThread.setName("dispatcher thread");
        dispatchThread.start();
    }

    private void shutdownDispatcher() {
        if (dispatchThread != null) {
            dispatchThread.interrupt();
            dispatcherSelector.wakeup();
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("关闭ServerSocket异常", e);
        }

        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            logger.error("关闭ServerSocketChannel异常", e);
        }
    }

    private void accept(final SelectionKey selectionKey) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            docker.accept(socketChannel);
        } catch (Exception e) {
            logger.error("分发传入连接异常", e);
        }
    }
}
