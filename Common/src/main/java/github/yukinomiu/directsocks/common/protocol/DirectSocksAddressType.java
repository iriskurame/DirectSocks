package github.yukinomiu.directsocks.common.protocol;

/**
 * Yukinomiu
 * 2017/7/29
 */
public class DirectSocksAddressType {
    public static final byte IPV4 = (byte) 0x00;
    public static final byte DOMAIN_NAME = (byte) 0x01;
    public static final byte IPV6 = (byte) 0x02;

    private DirectSocksAddressType() {
    }
}
