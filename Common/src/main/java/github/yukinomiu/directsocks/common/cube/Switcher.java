package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.LifeCycle;
import github.yukinomiu.directsocks.common.cube.api.NioHandle;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeStateException;
import github.yukinomiu.directsocks.common.cube.exception.SwitcherInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
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
    private final ReentrantLock lock;
    private final Selector switchSelector;

    private Thread switchThread;

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

        initSwitcher();

        state = State.RUNNING;
        logger.debug("Switcher成功启动");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPING;

        destroySwitcher();

        state = State.STOPED;
        logger.debug("Switcher成功关闭");
    }

    private void initSwitcher() {
        switchThread = new Thread(() -> {
            logger.debug("开始等待IO事件");

            while (true) {
                try {
                    int selectCount = switchSelector.select();

                    if (Thread.currentThread().isInterrupted()) {
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

                        if (selectionKey.isReadable()) {
                            handleRead(selectionKey);
                        }

                        if (selectionKey.isWritable()) {
                            handleWrite(selectionKey);
                        }
                    }

                } catch (CancelledKeyException e) {
                    logger.warn("连接断开");
                } catch (IOException e) {
                    logger.error("SwitchSelector IO异常", e);
                } catch (ClosedSelectorException e) {
                    logger.debug("SwitchSelector已经关闭");
                    break;
                } catch (Exception e) {
                    logger.debug("SwitchSelector异常");
                    break;
                }
            }

            logger.debug("结束等待IO事件");
        });

        switchThread.setName("switch thread");
        switchThread.start();
    }

    private void destroySwitcher() {
        if (switchThread != null) {
            switchThread.interrupt();
            switchSelector.wakeup();
        }

        try {
            switchSelector.close();
        } catch (IOException e) {
            logger.error("关闭SwitchSelector异常", e);
        }
    }

    private void handleRead(final SelectionKey selectionKey) {
        ByteBuffer readBuffer = byteBufferCachePool.get();
        ByteBuffer writeBuffer = byteBufferCachePool.get();

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        CubeContext cubeContext = (CubeContext) selectionKey.attachment();

        try {
            int n;
            try {
                n = socketChannel.read(readBuffer);
            } catch (IOException e) {
                cancelAndCloseSelectionKey(selectionKey);
                logger.warn("读取数据IO异常, 关闭连接", e);
                return;
            }

            if (n == -1) {
                cancelAndCloseSelectionKey(selectionKey);
                logger.debug("已到达流末尾, 关闭连接");
                return;
            }

            if (n == 0) {
                return;
            }

            readBuffer.flip();
            cubeContext.setReadBuffer(readBuffer);
            cubeContext.setWriteBuffer(writeBuffer);

            try {
                nioHandle.handleRead(cubeContext);
            } catch (Exception e) {
                cancelAndCloseSelectionKey(selectionKey);
                logger.error("NioHandle处理异常, 关闭连接", e);
                return;
            }

            if (cubeContext.isCancelFlag()) {
                cancelAndCloseSelectionKey(selectionKey);
            }
        } finally {
            cubeContext.setReadBuffer(null);
            byteBufferCachePool.returnBack(readBuffer);

            writeBuffer.flip();
            if (writeBuffer.remaining() == 0 || cubeContext.isCancelFlag()) { // no data to write
                cubeContext.setWriteBuffer(null);
                byteBufferCachePool.returnBack(writeBuffer);
            } else {
                selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
            }
        }
    }

    private void handleWrite(final SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        CubeContext cubeContext = (CubeContext) selectionKey.attachment();

        ByteBuffer writeBuffer = cubeContext.getWriteBuffer();
        try {
            try {
                socketChannel.write(writeBuffer);
            } catch (IOException e) {
                cancelAndCloseSelectionKey(selectionKey);
                logger.warn("写入数据IO异常, 关闭连接", e);
                return;
            }

            if (cubeContext.isCancelAfterWriteFlag()) {
                cancelAndCloseSelectionKey(selectionKey);
                return;
            }

            selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_WRITE));
        } finally {
            cubeContext.setWriteBuffer(null);
            byteBufferCachePool.returnBack(writeBuffer);
        }
    }

    private void cancelAndCloseSelectionKey(final SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        selectionKey.attach(null); // help GC
        selectionKey.cancel();
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.error("关闭SocketChannel IO异常", e);
        }
    }

    void accept(final SocketChannel socketChannel) {
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

        CubeContext cubeContext = new CubeContext(switchSelector, lock);

        lock.lock();
        switchSelector.wakeup();
        try {
            socketChannel.register(switchSelector, SelectionKey.OP_READ, cubeContext);
        } catch (ClosedChannelException e) {
            logger.warn("连接意外关闭", e);
        } finally {
            lock.unlock();
        }
    }
}
