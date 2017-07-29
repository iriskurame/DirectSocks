package github.yukinomiu.directsocks.common.protocol;

/**
 * Yukinomiu
 * 2017/7/29
 */
public class DirectSocksReply {
    public static final byte SUCCESS = (byte) 0x00;
    public static final byte AUTH_FAIL = (byte) 0x01;
    public static final byte CONNECTION_TYPE_NOT_SUPPORTED = (byte) 0x02;
    public static final byte NETWORK_UNREACHABLE = (byte) 0x03;
    public static final byte HOST_UNREACHABLE = (byte) 0x04;
    public static final byte GENERAL_FAIL = (byte) 0x09;

    private DirectSocksReply() {
    }
}
