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
            logger.error("init dispatcher IO exception", e);
            throw new CubeInitException("Dispatcher IO exception", e);
        }

        // bind server socket
        InetAddress bindAddress = cubeConfig.getBindAddress();
        int bindPort = cubeConfig.getBindPort();
        int backlog = cubeConfig.getBacklog();

        serverSocket = serverSocketChannel.socket();
        try {
            serverSocket.bind(new InetSocketAddress(bindAddress, bindPort), backlog);
        } catch (BindException e) {
            logger.error("bind local address exception: {}, the port {} may already in use", e.getMessage(), bindPort);
            throw new CubeInitException("bind local address exception", e);
        } catch (IOException e) {
            logger.error("bind IO exception", e);
            throw new CubeInitException("bind IO exception", e);
        }

        // register channel to selector
        try {
            serverSocketChannel.register(dispatcherSelector, SelectionKey.OP_ACCEPT);
        } catch (ClosedChannelException e) {
            logger.error("ServerSocketChannel already closed", e);
            throw new CubeInitException("ServerSocketChannel already closed", e);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startDispatcher();

        state = State.RUNNING;
        logger.debug("Dispatcher started");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownDispatcher();

        state = State.STOPPED;
        logger.debug("Dispatcher closed");
    }

    private void checkConfig(final CubeConfig cubeConfig) throws DispatcherInitException {
        if (cubeConfig == null) throw new DispatcherInitException("config can not be null");

        InetAddress bindAddress = cubeConfig.getBindAddress();
        if (bindAddress == null) throw new DispatcherInitException("bind address can not be null");
        if (!(bindAddress instanceof Inet4Address) && !(bindAddress instanceof Inet6Address))
            throw new DispatcherInitException("bind address type not supported");

        Integer bindPort = cubeConfig.getBindPort();
        if (bindPort == null) throw new DispatcherInitException("bind port can not be null");
        if (bindPort < 1 || bindPort > 65535) throw new DispatcherInitException("bind port must in range 1-65535");

        Integer backlog = cubeConfig.getBacklog();
        if (backlog == null) throw new DispatcherInitException("backlog can not be null");
    }

    private void startDispatcher() {
        dispatchThread = new Thread(() -> {
            logger.debug("start listening at address {}:{}", serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());

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
                    logger.warn("connection is closed");
                } catch (IOException e) {
                    logger.error("Dispatcher IO exception", e);
                } catch (Exception e) {
                    logger.debug("Dispatcher exception", e);
                    break;
                }
            }

            logger.debug("stop listening");
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
            logger.error("closing ServerSocket IO exception", e);
        }

        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            logger.error("closing ServerSocketChannel IO exception", e);
        }
    }

    private void accept(final SelectionKey selectionKey) {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        try {
            SocketChannel socketChannel = serverSocketChannel.accept();
            docker.accept(socketChannel);
        } catch (Exception e) {
            logger.error("dispatch request exception", e);
        }
    }
}
