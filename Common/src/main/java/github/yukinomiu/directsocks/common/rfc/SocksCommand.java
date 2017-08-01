package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/28
 */
public final class SocksCommand {
    public static final byte CONNECT = (byte) 0x01;
    public static final byte BIND = (byte) 0x02;
    public static final byte UDP_ASSOCIATE = (byte) 0x03;

    private SocksCommand() {
    }
}
