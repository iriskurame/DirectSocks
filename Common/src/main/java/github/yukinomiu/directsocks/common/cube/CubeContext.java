package github.yukinomiu.directsocks.common.cube;

import github.yukinomiu.directsocks.common.cube.api.CloseableAttachment;
import github.yukinomiu.directsocks.common.cube.cachepool.ByteBufferCachePool;
import github.yukinomiu.directsocks.common.cube.exception.NotYetWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;

/**
 * Yukinomiu
 * 2017/7/20
 */
public class CubeContext {
    private static final Logger logger = LoggerFactory.getLogger(CubeContext.class);

    private final SelectionKey selectionKey;
    private final Selector selector;
    private final Lock lock;
    private final ByteBufferCachePool byteBufferCachePool;

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private boolean writeFlag = false;
    private boolean cancelAfterWriteFlag = false;

    private CloseableAttachment attachment;

    CubeContext(final SelectionKey selectionKey,
                final Selector selector,
                final Lock lock,
                final ByteBufferCachePool byteBufferCachePool) {

        this.selectionKey = selectionKey;
        this.selector = selector;
        this.lock = lock;
        this.byteBufferCachePool = byteBufferCachePool;

        readBuffer = byteBufferCachePool.get();
        writeBuffer = byteBufferCachePool.get();
    }

    boolean isCancelAfterWriteFlag() {
        return cancelAfterWriteFlag;
    }

    ByteBuffer readyRead() {
        byteBufferCachePool.refresh(readBuffer);
        return readBuffer;
    }

    void finishWrite() {
        writeFlag = false;
        selectionKey.interestOps(selectionKey.interestOps() & (~SelectionKey.OP_WRITE));
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public ByteBuffer readyWrite() throws NotYetWriteException {
        if (writeFlag) {
            throw new NotYetWriteException();
        }

        writeFlag = true;
        selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);

        byteBufferCachePool.refresh(writeBuffer);
        return writeBuffer;
    }

    public void cancel() {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();

        selectionKey.cancel();
        selectionKey.attach(null); // help GC
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.error("关闭SocketChannel IO异常", e);
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
                logger.error("关闭Attachment异常", e);
            }
        }
    }

    public void cancelAfterWrite() {
        cancelAfterWriteFlag = true;
    }

    public void registerNewSocketChannel(final SocketChannel socketChannel, final CloseableAttachment attachment) throws IOException {
        if (socketChannel == null) throw new NullPointerException("SocketChannel为空");
        socketChannel.configureBlocking(false);

        SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
        CubeContext cubeContext = new CubeContext(selectionKey, selector, lock, byteBufferCachePool);
        selectionKey.attach(cubeContext);

        cubeContext.attach(attachment);
    }

    public void registerNewSocketChannel(final SocketChannel socketChannel) throws IOException {
        registerNewSocketChannel(socketChannel, null);
    }

    public void attach(final CloseableAttachment attachment) {
        this.attachment = attachment;
    }

    public CloseableAttachment attachment() {
        return attachment;
    }
}
