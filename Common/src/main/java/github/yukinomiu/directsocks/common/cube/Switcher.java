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
public final class Switcher implements LifeCycle {
    private static Logger logger = LoggerFactory.getLogger(Switcher.class);

    private LifeCycle.State state;

    private final CubeConfig cubeConfig;
    private final NioHandle nioHandle;
    private final ByteBufferCachePool readPool;
    private final ByteBufferCachePool writePool;
    private final ByteBufferCachePool framePool;
    private final Selector switcherSelector;
    private final Lock lock = new ReentrantLock();

    private Thread switchThread;

    private long acceptCount;

    Switcher(final CubeConfig cubeConfig,
             final NioHandle nioHandle,
             final ByteBufferCachePool readPool,
             final ByteBufferCachePool writePool,
             final ByteBufferCachePool framePool) throws SwitcherInitException {

        state = LifeCycle.State.NEW;
        checkConfig(cubeConfig);

        this.cubeConfig = cubeConfig;
        this.nioHandle = nioHandle;
        this.readPool = readPool;
        this.writePool = writePool;
        this.framePool = framePool;

        try {
            // init selector
            switcherSelector = Selector.open();
        } catch (IOException e) {
            logger.error("init Switcher IO exception", e);
            throw new SwitcherInitException("init Switcher IO exception", e);
        }
    }

    @Override
    public synchronized void start() {
        if (state != State.NEW) throw new CubeStateException();
        state = State.STARTING;

        startSwitcher();

        state = State.RUNNING;
        logger.debug("Switcher started");
    }

    @Override
    public synchronized void shutdown() {
        if (state != State.RUNNING) throw new CubeStateException();
        state = State.STOPPING;

        shutdownSwitcher();
        logger.debug("{}: acceptCount={}", this, acceptCount);

        state = State.STOPPED;
        logger.debug("Switcher closed");
    }

    void accept(final SocketChannel socketChannel) {
        acceptCount++;

        // init new socket channel
        try {
            configSocketChannel(socketChannel);
        } catch (IOException e) {
            logger.warn("config SocketChannel exception, connection will be closed", e);
            try {
                socketChannel.close();
            } catch (IOException ioe) {
                logger.error("closing SocketChannel IO exception", ioe);
            }

            return;
        }

        lock.lock();
        switcherSelector.wakeup();
        try {
            CubeContext cubeContext = register(socketChannel);
            if (cubeContext != null) {
                cubeContext.readyRead(); // default ready read
            }
        } finally {
            lock.unlock();
        }
    }

    CubeContext registerNew() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        try {
            configSocketChannel(socketChannel);
        } catch (IOException e) {
            logger.error("config SocketChannel IO exception", e);
            socketChannel.close();
            throw e;
        }

        SelectionKey selectionKey;
        try {
            selectionKey = socketChannel.register(switcherSelector, 0);
        } catch (IOException e) {
            logger.error("register SocketChannel IO exception", e);
            socketChannel.close();
            throw e;
        }

        CubeContext cubeContext = new CubeContext(this, selectionKey, readPool, writePool, framePool);
        selectionKey.attach(cubeContext);

        return cubeContext;
    }

    private void checkConfig(final CubeConfig cubeConfig) throws SwitcherInitException {
        if (cubeConfig == null) throw new SwitcherInitException("config can not be null");

        Boolean tcpNoDelay = cubeConfig.getTcpNoDelay();
        if (tcpNoDelay == null) throw new SwitcherInitException("TcpNoDelay can not be null");

        Boolean tcpKeepAlive = cubeConfig.getTcpKeepAlive();
        if (tcpKeepAlive == null) throw new SwitcherInitException("TcpKeepAlive can not be null");
    }

    private void startSwitcher() {
        switchThread = new Thread(() -> {
            logger.debug("start waiting switcher IO events");

            CubeContext cubeContext = null;
            while (true) {
                try {
                    int selectCount = switcherSelector.select();

                    if (Thread.currentThread().isInterrupted()) {
                        // close this switcher
                        Set<SelectionKey> selectionKeys = switcherSelector.keys();
                        for (SelectionKey selectionKey : selectionKeys) {
                            cubeContext = (CubeContext) selectionKey.attachment();
                            cubeContext.close();
                        }

                        switcherSelector.close();
                        break;
                    }

                    lock.lock();
                    lock.unlock();

                    if (selectCount == 0) {
                        continue;
                    }

                    Set<SelectionKey> sets = switcherSelector.selectedKeys();
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

                } catch (IOException e) {
                    logger.error("SwitcherSelector IO exception", e);

                } catch (ClosedSelectorException e) {
                    logger.error("SwitcherSelector is closed");
                    break;

                } catch (CancelledKeyException e) {
                    logger.debug("connection is closed");
                    cubeContext.close();

                } catch (Exception e) {
                    logger.error("unknown switcher exception", e);
                    try {
                        switcherSelector.close();
                    } catch (IOException ioe) {
                        logger.error("closing switcher selector IO exception", e);
                    }
                    break;
                }
            }

            logger.debug("stop waiting switcher IO events");
        });

        switchThread.setName("switcher thread");
        switchThread.start();
    }

    private void shutdownSwitcher() {
        if (switchThread != null) {
            switchThread.interrupt();
            switcherSelector.wakeup();
        }
    }

    private void configSocketChannel(final SocketChannel socketChannel) throws IOException {
        socketChannel.configureBlocking(false);

        if (cubeConfig.getTcpNoDelay()) {
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        }

        if (cubeConfig.getTcpKeepAlive()) {
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        }
    }

    private CubeContext register(final SocketChannel socketChannel) {
        SelectionKey selectionKey;
        try {
            selectionKey = socketChannel.register(switcherSelector, 0);
        } catch (ClosedChannelException e) {
            logger.debug("connection is closed");
            return null;
        } catch (ClosedSelectorException e) {
            logger.error("SwitcherSelector is closed");
            return null;
        } catch (CancelledKeyException e) {
            logger.error("socket channel is canceled");
            return null;
        }

        CubeContext cubeContext = new CubeContext(this, selectionKey, readPool, writePool, framePool);
        selectionKey.attach(cubeContext);

        return cubeContext;
    }

    private void handleWrite(final SelectionKey selectionKey, final CubeContext cubeContext) {
        cubeContext.finishWrite();
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        final ByteBuffer writeBuffer = cubeContext.getWriteBuffer();
        writeBuffer.flip();
        if (writeBuffer.hasRemaining()) {
            try {
                socketChannel.write(writeBuffer);
            } catch (IOException e) {
                logger.warn("write exception: {}, connection will be closed", e.getMessage());
                cubeContext.close();
                return;
            }
        }

        if (cubeContext.isAfterWriteCallback()) {
            cubeContext.afterWriteCallback();
            cubeContext.finishAfterWriteCallback();
        }

        if (cubeContext.isReadAfterWrite()) {
            cubeContext.readyRead();
            cubeContext.finishReadAfterWrite();
        }

        if (cubeContext.isCloseAfterWrite()) {
            cubeContext.close();
            cubeContext.finishCloseAfterWrite();
        }
    }

    private void handleRead(final SelectionKey selectionKey, final CubeContext cubeContext) {
        cubeContext.finishRead();
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        final ByteBuffer readBuffer = cubeContext.getReadBuffer();
        int n;
        try {
            n = socketChannel.read(readBuffer);
        } catch (IOException e) {
            logger.warn("read exception: {}, connection will be closed", e.getMessage());
            cubeContext.close();
            return;
        }

        if (n == -1) {
            logger.debug("end of stream, connection will be closed");
            cubeContext.close();
            return;
        }

        if (n == 0) {
            logger.warn("can not read any data from read IO event");
            return;
        }

        readBuffer.flip();
        try {
            nioHandle.handleRead(cubeContext);
        } catch (CancelledKeyException e) {
            logger.debug("connection is closed while handle read");
            cubeContext.close();
        } catch (Exception e) {
            logger.error("NioHandle handle read exception, connection will be closed", e);
            cubeContext.close();
        }
    }

    private void handleConnect(final SelectionKey selectionKey, final CubeContext cubeContext) {
        cubeContext.finishConnect();
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        try {
            socketChannel.finishConnect();

            // connection success
            try {
                nioHandle.handleConnectSuccess(cubeContext);
            } catch (CancelledKeyException e) {
                logger.debug("connection is closed while handle connection success");
                cubeContext.close();
            } catch (Exception e) {
                logger.error("NioHandle handle connect success exception, connection will be closed", e);
                cubeContext.close();
            }
        } catch (IOException ioe) {
            if (logger.isDebugEnabled()) {
                String remoteAddressString = cubeContext.getRemoteAddress().getHostAddress();
                String remoteIPString = String.valueOf(cubeContext.getRemotePort());
                logger.warn("connect remote host {}:{} IO exception: {}", remoteAddressString, remoteIPString, ioe.getMessage());
            }

            try {
                CubeConnectionException cubeConnectionException = new CubeConnectionException("connection IO exception: " + ioe.getMessage(), ioe);
                nioHandle.handleConnectFail(cubeContext, cubeConnectionException);
            } catch (CancelledKeyException e) {
                logger.debug("connection is closed while handle connection fail");
                cubeContext.close();
            } catch (Exception e) {
                logger.error("NioHandle handle connection fail exception, connection will be closed", e);
                cubeContext.close();
            }
        }
    }
}
