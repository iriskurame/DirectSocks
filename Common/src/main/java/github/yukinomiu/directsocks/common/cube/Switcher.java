package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import github.yukinomiu.directsocks.common.cube.exception.SwitcherInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private final ByteBufferCachePool byteBufferCachePool;
    private final NioHandle nioHandle;
    private final Lock lock;
    private final Selector switchSelector;

    private Thread switchThread;

    private long acceptCount;

    Switcher(final ByteBufferCachePool byteBufferCachePool, final NioHandle nioHandle) throws SwitcherInitException {
        state = LifeCycle.State.NEW;

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

            while (true) {
                try {
                    int selectCount = switchSelector.select();

                    if (Thread.currentThread().isInterrupted()) {
                        // close this switcher
                        Set<SelectionKey> selectionKeys = switchSelector.keys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            CubeContext cubeContext = (CubeContext) selectionKey.attachment();
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

                        if (!selectionKey.isValid()) {
                            continue;
                        }

                        if (selectionKey.isWritable()) {
                            handleWrite(selectionKey);
                        }

                        if (selectionKey.isReadable()) {
                            handleRead(selectionKey);
                        }

                        if (selectionKey.isConnectable()) {
                            handleConnect(selectionKey);
                        }
                    }

                } catch (CancelledKeyException e) {
                    logger.warn("连接已断开");
                } catch (IOException e) {
                    logger.error("Switcher IO异常", e);
                } catch (Exception e) {
                    logger.debug("Switcher异常", e);
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

    private void handleRead(final SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        CubeContext cubeContext = (CubeContext) selectionKey.attachment();

        ByteBuffer readBuffer = cubeContext.readyRead();

        int n;
        try {
            n = socketChannel.read(readBuffer);
        } catch (IOException e) {
            cubeContext.cancel();
            logger.warn("读取数据IO异常, 关闭连接", e);
            return;
        }

        if (n == -1) {
            cubeContext.cancel();
            logger.debug("已到达流末尾, 关闭连接");
            return;
        }

        if (n == 0) {
            return;
        }

        readBuffer.flip();
        try {
            nioHandle.handleRead(cubeContext);
        } catch (CancelledKeyException e) {
            logger.info("连接意外关闭", e);
            cubeContext.cancel();
        } catch (Exception e) {
            cubeContext.cancel();
            logger.error("NioHandle处理读取流程异常, 关闭连接", e);
        }
    }

    private void handleWrite(final SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        CubeContext cubeContext = (CubeContext) selectionKey.attachment();

        ByteBuffer writeBuffer = cubeContext.getWriteBuffer();
        writeBuffer.flip();
        try {
            socketChannel.write(writeBuffer);
        } catch (IOException e) {
            cubeContext.cancel();
            logger.error("NioHandle处理写入流程异常, 关闭连接", e);
            return;
        }

        if (cubeContext.isCancelAfterWriteFlag()) {
            cubeContext.cancel();
            return;
        }

        cubeContext.finishWrite();
    }

    private void handleConnect(final SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        CubeContext cubeContext = (CubeContext) selectionKey.attachment();

        try {
            boolean connected = socketChannel.finishConnect();
            if (!connected) throw new RuntimeException("连接未知异常");

            // connection success
            try {
                nioHandle.handleConnectedSuccess(cubeContext);
            } catch (Exception e) {
                cubeContext.cancel();
                logger.error("NioHandle处理连接成功流程异常, 关闭连接", e);
            }
        } catch (IOException e) {
            // connection fail
            CubeConnectionException cubeConnectionException = new CubeConnectionException("连接异常", e);
            try {
                nioHandle.handleConnectedFail(cubeContext, cubeConnectionException);
            } catch (Exception e2) {
                cubeContext.cancel();
                logger.error("NioHandle处理连接失败流程异常, 关闭连接", e2);
                return;
            }
        }

        cubeContext.finishConnect();
    }

    void accept(final SocketChannel socketChannel) {
        acceptCount++;

        try {
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            try {
                socketChannel.close();
                logger.warn("配置连接异步模式异常, 已关闭连接", e);
            } catch (IOException ioe) {
                logger.error("关闭连接异常", ioe);
            }

            return;
        }

        lock.lock();
        switchSelector.wakeup();
        try {
            SelectionKey selectionKey = socketChannel.register(switchSelector, SelectionKey.OP_READ);

            CubeContext cubeContext = new CubeContext(selectionKey, switchSelector, lock, byteBufferCachePool);
            selectionKey.attach(cubeContext);
        } catch (ClosedChannelException e) {
            logger.warn("连接意外关闭", e);
        } finally {
            lock.unlock();
        }
    }
}
