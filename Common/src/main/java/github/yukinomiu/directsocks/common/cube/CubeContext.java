package github.yukinomiu.directsocks.common.cube;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Yukinomiu
 * 2017/7/20
 */
public class CubeContext {
    private final Selector selector;
    private final ReentrantLock lock;

    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private boolean cancelAfterWriteFlag = false;
    private boolean cancelFlag = false;
    private Object attachment;

    CubeContext(final Selector selector, final ReentrantLock lock) {
        this.selector = selector;
        this.lock = lock;
    }

    void setReadBuffer(final ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }

    void setWriteBuffer(final ByteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    boolean isCancelAfterWriteFlag() {
        return cancelAfterWriteFlag;
    }

    boolean isCancelFlag() {
        return cancelFlag;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public void cancelAfterWrite() {
        cancelAfterWriteFlag = true;
    }

    public void cancel() {
        cancelFlag = true;
    }

    public void attach(Object object) {
        attachment = object;
    }

    public Object attachment() {
        return attachment;
    }

    public void register(final SocketChannel socketChannel, final Object attachment) throws IOException {
        if (socketChannel == null) throw new NullPointerException("SocketChannel为空");

        socketChannel.configureBlocking(false);
        CubeContext cubeContext = new CubeContext(selector, lock);
        cubeContext.attach(attachment);

        lock.lock();
        selector.wakeup();
        try {
            socketChannel.register(selector, SelectionKey.OP_READ, cubeContext);
        } finally {
            lock.unlock();
        }
    }

    public void register(final SocketChannel socketChannel) throws IOException {
        register(socketChannel, null);
    }
}
