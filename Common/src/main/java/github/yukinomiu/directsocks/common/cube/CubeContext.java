package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.CubeConnectionException;
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
public class CubeContext {
    private static final Logger logger = LoggerFactory.getLogger(CubeContext.class);

    private final Switcher switcher;
    private final SelectionKey selectionKey;
    private final ByteBufferCachePool byteBufferCachePool;

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private boolean readFlag = false;
    private boolean writeFlag = false;
    private boolean cancelFlag = false;

    private boolean cancelAfterWriteFlag = false;
    private boolean readAfterWriteFlag = false;

    private boolean contextReadAfterWriteFlag = false;
    private CubeContext context;

    private CloseableAttachment attachment;

    CubeContext(final Switcher switcher,
                final SelectionKey selectionKey,
                final ByteBufferCachePool byteBufferCachePool) {

        this.switcher = switcher;
        this.selectionKey = selectionKey;
        this.byteBufferCachePool = byteBufferCachePool;

        readBuffer = byteBufferCachePool.get();
        writeBuffer = byteBufferCachePool.get();
    }

    boolean getAndTurnOffCancelAfterWriteFlag() {
        boolean old = cancelAfterWriteFlag;
        cancelAfterWriteFlag = false;
        return old;
    }

    boolean getAndTurnOffReadAfterWriteFlag() {
        boolean old = readAfterWriteFlag;
        readAfterWriteFlag = false;
        return old;
    }

    boolean getAndTurnOffContextReadAfterWriteFlag() {
        boolean old = contextReadAfterWriteFlag;
        contextReadAfterWriteFlag = false;
        return old;
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

    void finishContextReadAfterWrite() {
        context.readyRead();
    }

    public void readyRead() {
        if (readFlag) {
            return;
        }

        readFlag = true;
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);

        byteBufferCachePool.refresh(readBuffer);
    }

    public ByteBuffer readyWrite() {
        if (writeFlag) {
            return writeBuffer;
        }

        writeFlag = true;
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);

        byteBufferCachePool.refresh(writeBuffer);
        return writeBuffer;
    }

    public CubeContext readyConnect(final SocketAddress remoteAddress) throws CubeConnectionException {
        if (remoteAddress == null) throw new NullPointerException("SocketAddress can not be null");

        CubeContext newCubeContext = switcher.registerNewSocketChannel();
        SelectionKey newSelectionKey = newCubeContext.selectionKey;
        SocketChannel newSocketChannel = (SocketChannel) newSelectionKey.channel();

        newSelectionKey.interestOps(newSelectionKey.interestOps() | SelectionKey.OP_CONNECT);
        try {
            newSocketChannel.connect(remoteAddress);
        } catch (Exception e) {
            newCubeContext.cancel();
            throw new CubeConnectionException("connection exception", e);
        }

        return newCubeContext;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public void cancel() {
        if (cancelFlag) return;

        cancelFlag = true;
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        selectionKey.cancel();
        selectionKey.attach(null); // help GC
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.error("closing SocketChannel IO exception", e);
        }

        if (readBuffer != null) {
            byteBufferCachePool.returnBack(readBuffer);
            readBuffer = null;
        }

        if (writeBuffer != null) {
            byteBufferCachePool.returnBack(writeBuffer);
            writeBuffer = null;
        }

        if (attachment != null) {
            try {
                attachment.close();
            } catch (Exception e) {
                logger.error("closing attachment exception", e);
            }
        }
    }

    public void cancelAfterWrite() {
        cancelAfterWriteFlag = true;
    }

    public void readAfterWrite() {
        readAfterWriteFlag = true;
    }

    public void contextReadAfterWrite(final CubeContext cubeContext) {
        if (cubeContext == null) throw new NullPointerException("CubeContext can not be null");

        contextReadAfterWriteFlag = true;
        context = cubeContext;
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
