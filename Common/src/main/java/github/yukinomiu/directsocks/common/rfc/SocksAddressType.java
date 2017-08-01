package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/28
 */
public final class SocksAddressType {
    public static final byte IPV4 = (byte) 0x01;
    public static final byte DOMAIN_NAME = (byte) 0x03;
    public static final byte IPV6 = (byte) 0x04;

    private SocksAddressType() {
    }
}
