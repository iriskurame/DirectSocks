package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
import github.yukinomiu.directsocks.common.cube.exception.CubeRuntimeException;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import github.yukinomiu.directsocks.common.cube.exception.SwitcherInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Yukinomiu
 * 2017/7/19
 */
public class Switcher implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Switcher.class);

    private LifeCycle.State state;

    private final CubeConfig cubeConfig;
    private final ByteBufferCachePool byteBufferCachePool;
    private final NioHandle nioHandle;
    private final Lock lock;
    private final Selector switchSelector;

    private Thread switchThread;

    private long acceptCount;

    Switcher(final CubeConfig cubeConfig, final ByteBufferCachePool byteBufferCachePool, final NioHandle nioHandle) throws SwitcherInitException {
        state = LifeCycle.State.NEW;
        checkConfig(cubeConfig);

        this.cubeConfig = cubeConfig;
        this.byteBufferCachePool = byteBufferCachePool;
        this.nioHandle = nioHandle;
        lock = new ReentrantLock();

        try {
            // init selector
            switchSelector = Selector.open();
        } catch (IOException e) {
            logger.error("Switcher IO异常", e);
            throw new SwitcherInitException("Switcher IO异常", e);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startSwitcher();

        state = State.RUNNING;
        logger.debug("Switcher成功启动");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownSwitcher();
        logger.debug("{}: acceptCount={}", this, acceptCount);

        state = State.STOPPED;
        logger.debug("Switcher成功关闭");
    }

    private void startSwitcher() {
        switchThread = new Thread(() -> {
            logger.debug("开始等待Switcher IO事件");

            CubeContext cubeContext = null;
            while (true) {
                try {
                    int selectCount = switchSelector.select();

                    if (Thread.currentThread().isInterrupted()) {
                        // close this switcher
                        Set<SelectionKey> selectionKeys = switchSelector.keys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            cubeContext = (CubeContext) selectionKey.attachment();
                            if (cubeContext != null) {
                                cubeContext.cancel();
                            }
                        }

                        switchSelector.close();
                        break;
                    }

                    lock.lock();
                    lock.unlock();

                    if (selectCount == 0) {
                        continue;
                    }

                    Set<SelectionKey> sets = switchSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = sets.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();
                        cubeContext = (CubeContext) selectionKey.attachment();

                        if (selectionKey.isValid() && selectionKey.isWritable()) {
                            handleWrite(selectionKey, cubeContext);
                        }

                        if (selectionKey.isValid() && selectionKey.isReadable()) {
                            handleRead(selectionKey, cubeContext);
                        }

                        if (selectionKey.isValid() && selectionKey.isConnectable()) {
                            handleConnect(selectionKey, cubeContext);
                        }
                    }

                } catch (ClosedSelectorException e) {
                    logger.error("SwitchSelector意外关闭");
                    break;

                } catch (IOException e) {
                    logger.error("Switcher IO异常", e);
                    if (cubeContext != null) {
                        cubeContext.cancel();
                    }

                } catch (CancelledKeyException e) {
                    logger.debug("连接被关闭");
                    if (cubeContext != null) {
                        cubeContext.cancel();
                    }

                } catch (Exception e) {
                    logger.error("Switcher异常", e);
                    if (cubeContext != null) {
                        cubeContext.cancel();
                    }
                    break;
                }
            }

            logger.debug("结束等待Switcher IO事件");
        });

        switchThread.setName("switcher thread");
        switchThread.start();
    }

    private void shutdownSwitcher() {
        if (switchThread != null) {
            switchThread.interrupt();
            switchSelector.wakeup();
        }
    }

    private void handleRead(final SelectionKey selectionKey, final CubeContext cubeContext) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();

        int n;
        try {
            n = socketChannel.read(readBuffer);
        } catch (IOException e) {
            logger.error("NioHandle处理读取流程异常, 关闭连接", e.getMessage());
            cubeContext.cancel();
            return;
        }

        if (n == -1) {
            logger.debug("远端已关闭连接, 关闭连接");
            cubeContext.cancel();
            return;
        }

        if (n == 0) {
            logger.warn("读取长度为0");
            return;
        }

        readBuffer.flip();
        try {
            nioHandle.handleRead(cubeContext);
        } catch (CancelledKeyException e) {
            logger.debug("连接被关闭");
            cubeContext.cancel();
        } catch (Exception e) {
            logger.error("NioHandle处理读取流程异常, 关闭连接", e);
            cubeContext.cancel();
        }

        cubeContext.finishRead();
    }

    private void handleWrite(final SelectionKey selectionKey, final CubeContext cubeContext) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        final ByteBuffer writeBuffer = cubeContext.getWriteBuffer();
        writeBuffer.flip();
        try {
            socketChannel.write(writeBuffer);
        } catch (IOException e) {
            logger.error("NioHandle处理写入流程异常, 关闭连接", e.getMessage());
            cubeContext.cancel();
            return;
        }

        if (cubeContext.getAndTurnOffCancelAfterWriteFlag()) {
            cubeContext.cancel();
            return;
        }

        if (cubeContext.getAndTurnOffReadAfterWriteFlag()) {
            try {
                cubeContext.readyRead();
            } catch (CancelledKeyException e) {
                logger.debug("连接被关闭");
                cubeContext.cancel();
                return;
            }
        }

        if (cubeContext.getAndTurnOffContextReadAfterWriteFlag()) {
            try {
                cubeContext.finishContextReadAfterWrite();
            } catch (CancelledKeyException e) {
                logger.debug("连接被关闭");
                cubeContext.cancel();
                return;
            }
        }

        try {
            cubeContext.finishWrite();
        } catch (CancelledKeyException e) {
            logger.debug("连接被关闭");
            cubeContext.cancel();
        }
    }

    private void handleConnect(final SelectionKey selectionKey, final CubeContext cubeContext) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        try {
            boolean connected = socketChannel.finishConnect();
            if (!connected) throw new RuntimeException("连接未知异常");

            // connection success
            try {
                nioHandle.handleConnectedSuccess(cubeContext);
            } catch (Exception e) {
                logger.error("NioHandle处理连接成功流程异常, 关闭连接", e);
                cubeContext.cancel();
            }
        } catch (IOException ioe) {
            // connection fail
            if (logger.isWarnEnabled()) {
                String remoteAddressString = cubeContext.getRemoteAddress().getHostAddress();
                String remoteIPString = String.valueOf(cubeContext.getRemotePort());
                logger.warn("连接远程主机 {}:{} 异常", remoteAddressString, remoteIPString);
            }

            CubeConnectionException cubeConnectionException = new CubeConnectionException("连接异常", ioe);
            try {
                nioHandle.handleConnectedFail(cubeContext, cubeConnectionException);
            } catch (Exception e) {
                logger.error("NioHandle处理连接失败流程异常, 关闭连接", e);
                cubeContext.cancel();
                return;
            }
        }

        cubeContext.finishConnect();
    }

    private void configSocketChannel(final SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);

        if (cubeConfig.getTcpNoDelay()) {
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        }
    }

    void accept(final SocketChannel socketChannel) {
        acceptCount++;

        try {
            configSocketChannel(socketChannel);
        } catch (IOException e) {
            try {
                logger.warn("配置SocketChannel异常, 关闭连接", e);
                socketChannel.close();
            } catch (IOException ioe) {
                logger.error("关闭连接异常", ioe);
            }

            return;
        }

        CubeContext cubeContext = null;
        lock.lock();
        switchSelector.wakeup();
        try {
            SelectionKey selectionKey = socketChannel.register(switchSelector, 0);

            cubeContext = new CubeContext(this, selectionKey, byteBufferCachePool);
            selectionKey.attach(cubeContext);

            cubeContext.readyRead(); // default ready read
        } catch (ClosedChannelException e) {
            logger.debug("连接被关闭", e);
        } catch (CancelledKeyException e) {
            logger.warn("通道意外取消");
            if (cubeContext != null) {
                cubeContext.cancel();
            }
        } catch (ClosedSelectorException e) {
            logger.warn("SwitcherSelector已经关闭", e);
        } finally {
            lock.unlock();
        }
    }

    CubeContext registerNewSocketChannel() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            configSocketChannel(socketChannel);
            SelectionKey selectionKey = socketChannel.register(switchSelector, 0);

            CubeContext cubeContext = new CubeContext(this, selectionKey, byteBufferCachePool);
            selectionKey.attach(cubeContext);
            return cubeContext;
        } catch (IOException e) {
            throw new CubeRuntimeException("配置SocketChannel异常", e);
        }
    }

    private void checkConfig(final CubeConfig cubeConfig) throws SwitcherInitException {
        if (cubeConfig == null) throw new SwitcherInitException("配置不能为空");

        Boolean tcpNoDelay = cubeConfig.getTcpNoDelay();
        if (tcpNoDelay == null) throw new SwitcherInitException("TcpNoDelay不能为空");

        Boolean tcpKeepAlive = cubeConfig.getTcpKeepAlive();
        if (tcpKeepAlive == null) throw new SwitcherInitException("TcpKeepAlive不能为空");
    }
}
