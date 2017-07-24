package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/15
 */
public class SocksResponse {
    public static final byte REP_SUCCEEDED = (byte) 0x00;
    public static final byte REP_GENERAL_SERVER_FAILURE = (byte) 0x01;
    public static final byte REP_CONNECTION_NOT_ALLOWED = (byte) 0x02;
    public static final byte REP_NETWORK_UNREACHABLE = (byte) 0x03;
    public static final byte REP_HOST_UNREACHABLE = (byte) 0x04;
    public static final byte REP_CONNECTION_REFUSED = (byte) 0x05;
    public static final byte REP_TTL_EXPIRED = (byte) 0x06;
    public static final byte REP_COMMAND_NOT_SUPPORTED = (byte) 0x07;
    public static final byte REP_ADDRESS_TYPE_NOT_SUPPORTED = (byte) 0x08;

    public static final byte RESERVED = (byte) 0x00;

    public static final byte ATYP_IPV4 = (byte) 0x01;
    public static final byte ATYP_DOMAIN_NAME = (byte) 0x03;
    public static final byte ATYP_IPV6 = (byte) 0x04;

    private SocksResponse() {
    }
}
