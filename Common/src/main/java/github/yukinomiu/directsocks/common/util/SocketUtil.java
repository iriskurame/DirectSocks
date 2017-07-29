package github.yukinomiu.directsocks.common.util;

import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

/**
 * Yukinomiu
 * 2017/7/13
 */
public final class SocketUtil {
    private SocketUtil() {
    }

    public static int socketHashCode(final Socket socket) {
        if (socket == null) return 0;

        InetAddress localAddress = socket.getLocalAddress();
        int localPort = socket.getLocalPort();
        InetAddress address = socket.getInetAddress();
        int port = socket.getPort();

        StringBuilder sb = new StringBuilder(43);
        sb.append(localAddress.getHostAddress()).append(":");
        sb.append(localPort).append("-");
        sb.append(address.getHostAddress()).append(":");
        sb.append(port);

        return sb.toString().hashCode();
    }

    public static String toString(final Socket socket) {
        if (socket == null) throw new NullPointerException();

        String localAddress = socket.getLocalAddress().getHostAddress();
        int localPort = socket.getLocalPort();
        String remoteAddress = socket.getInetAddress().getHostAddress();
        int remotePort = socket.getPort();

        return String.format("Local<%s:%s> <==========> Remote<%s:%s>", localAddress, localPort, remoteAddress, remotePort);
    }

    public static String getRemoteIPString(final Socket socket) {
        return socket.getInetAddress().getHostAddress();
    }

    public static String getLocalIPString(final Socket socket) {
        return socket.getLocalAddress().getHostAddress();
    }

    public static String toString(final SocketChannel socketChannel) {
        return toString(socketChannel.socket());
    }

    public static String getRemoteIPString(final SocketChannel socketChannel) {
        return getRemoteIPString(socketChannel.socket());
    }

    public static String getLocalIPString(final SocketChannel socketChannel) {
        return getLocalIPString(socketChannel.socket());
    }
}
