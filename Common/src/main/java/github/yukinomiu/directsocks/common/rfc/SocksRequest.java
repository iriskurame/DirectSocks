package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/15
 */
public class SocksRequest {
    public static final byte COMMAND_CONNECT = (byte) 0x01;
    public static final byte COMMAND_BIND = (byte) 0x02;
    public static final byte COMMAND_UDP_ASSOCIATE = (byte) 0x03;

    public static final byte RESERVED = (byte) 0x00;

    public static final byte ATYP_IPV4 = (byte) 0x01;
    public static final byte ATYP_DOMAIN_NAME = (byte) 0x03;
    public static final byte ATYP_IPV6 = (byte) 0x04;

    private SocksRequest() {
    }
}
