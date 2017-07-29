package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/15
 */
public class SocksReply {
    public static final byte SUCCEEDED = (byte) 0x00;
    public static final byte GENERAL_SERVER_FAILURE = (byte) 0x01;
    public static final byte CONNECTION_NOT_ALLOWED = (byte) 0x02;
    public static final byte NETWORK_UNREACHABLE = (byte) 0x03;
    public static final byte HOST_UNREACHABLE = (byte) 0x04;
    public static final byte CONNECTION_REFUSED = (byte) 0x05;
    public static final byte TTL_EXPIRED = (byte) 0x06;
    public static final byte COMMAND_NOT_SUPPORTED = (byte) 0x07;
    public static final byte ADDRESS_TYPE_NOT_SUPPORTED = (byte) 0x08;

    private SocksReply() {
    }
}
