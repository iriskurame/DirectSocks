package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
import github.yukinomiu.directsocks.common.cube.exception.CubeRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Yukinomiu
 * 2017/7/20
 */
public final class CubeContext {
    private static final Logger logger = LoggerFactory.getLogger(CubeContext.class);

    private final Switcher switcher;
    private final SelectionKey selectionKey;
    private final ByteBufferCachePool readPool;
    private final ByteBufferCachePool writePool;
    private final ByteBufferCachePool framePool;

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private ByteBuffer frameBuffer;

    private boolean readFlag = false;
    private boolean writeFlag = false;
    private boolean closeFlag = false;

    private boolean closeAfterWriteFlag = false;
    private boolean readAfterWriteFlag = false;

    private boolean afterWriteCallbackFlag = false;
    private AfterWriteCallback afterWriteCallback;
    private Object arg;

    private CloseableAttachment attachment;

    CubeContext(final Switcher switcher,
                final SelectionKey selectionKey,
                final ByteBufferCachePool readPool,
                final ByteBufferCachePool writePool,
                final ByteBufferCachePool framePool) {

        this.switcher = switcher;
        this.selectionKey = selectionKey;
        this.readPool = readPool;
        this.writePool = writePool;
        this.framePool = framePool;

        readBuffer = readPool.get();
        writeBuffer = writePool.get();
        frameBuffer = framePool.get();
    }

    void finishRead() {
        readFlag = false;
        selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_READ));
    }

    void finishWrite() {
        writeFlag = false;
        selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_WRITE));
    }

    void finishConnect() {
        selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_CONNECT));
    }

    boolean isCloseAfterWrite() {
        return closeAfterWriteFlag;
    }

    void finishCloseAfterWrite() {
        closeAfterWriteFlag = false;
    }

    boolean isReadAfterWrite() {
        return readAfterWriteFlag;
    }

    void finishReadAfterWrite() {
        readAfterWriteFlag = false;
    }

    boolean isAfterWriteCallback() {
        return afterWriteCallbackFlag;
    }

    void finishAfterWriteCallback() {
        afterWriteCallbackFlag = false;
    }

    void afterWriteCallback() {
        afterWriteCallback.doCallback(arg);
    }

    public void readyRead() {
        if (readFlag) {
            return;
        }

        readFlag = true;
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        readPool.refresh(readBuffer);
    }

    public ByteBuffer readyWrite() {
        if (writeFlag) {
            return writeBuffer;
        }

        writeFlag = true;
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        writePool.refresh(writeBuffer);
        return writeBuffer;
    }

    public void cancelReadyWrite() {
        finishWrite();
    }

    public CubeContext readyConnect(final SocketAddress remoteAddress) throws CubeConnectionException {
        if (remoteAddress == null) throw new NullPointerException("remote SocketAddress can not be null");
        CubeContext newCubeContext;
        try {
            newCubeContext = switcher.registerNew();
        } catch (IOException e) {
            logger.error("register new SocketChannel exception", e);
            throw new CubeConnectionException("register new SocketChannel exception: " + e.getMessage(), e);
        }

        SelectionKey newSelectionKey = newCubeContext.selectionKey;
        newSelectionKey.interestOps(newSelectionKey.interestOps() | SelectionKey.OP_CONNECT);
        SocketChannel newSocketChannel = (SocketChannel) newSelectionKey.channel();
        try {
            newSocketChannel.connect(remoteAddress);
        } catch (IOException e) {
            newCubeContext.close();
            throw new CubeConnectionException("SocketChannel connection IO exception: " + e.getMessage(), e);
        }

        return newCubeContext;
    }

    public void closeAfterWrite() {
        closeAfterWriteFlag = true;
    }

    public void readAfterWrite() {
        readAfterWriteFlag = true;
    }

    public void setAfterWriteCallback(AfterWriteCallback afterWriteCallback, Object arg) {
        if (afterWriteCallbackFlag) throw new CubeRuntimeException("after write callback has already been set");
        afterWriteCallbackFlag = true;
        this.afterWriteCallback = afterWriteCallback;
        this.arg = arg;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public ByteBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public void close() {
        if (closeFlag) return;
        closeFlag = true;

        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        selectionKey.cancel();
        selectionKey.attach(null); // help GC

        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.error("closing SocketChannel IO exception", e);
        }

        if (readBuffer != null) {
            readPool.returnBack(readBuffer);
            readBuffer = null;
        }

        if (writeBuffer != null) {
            writePool.returnBack(writeBuffer);
            writeBuffer = null;
        }

        if (frameBuffer != null) {
            framePool.returnBack(frameBuffer);
            frameBuffer = null;
        }

        if (attachment != null) {
            try {
                attachment.close();
            } catch (Exception e) {
                logger.error("closing attachment exception", e);
            }
        }
    }

    public boolean isClosed() {
        return closeFlag;
    }

    public InetAddress getRemoteAddress() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        return socketChannel.socket().getInetAddress();
    }

    public int getRemotePort() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        return socketChannel.socket().getPort();
    }

    public InetAddress getLocalAddress() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        return socketChannel.socket().getLocalAddress();
    }

    public int getLocalPort() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        return socketChannel.socket().getLocalPort();
    }

    public void attach(final CloseableAttachment attachment) {
        this.attachment = attachment;
    }

    public CloseableAttachment attachment() {
        return attachment;
    }
}
