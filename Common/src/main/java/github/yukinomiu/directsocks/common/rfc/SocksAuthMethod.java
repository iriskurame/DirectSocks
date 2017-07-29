package github.yukinomiu.directsocks.common.rfc;

/**
 * Yukinomiu
 * 2017/7/15
 */
public class SocksAuthMethod {
    public static final byte NO_AUTH = (byte) 0x00;
    public static final byte NO_ACCEPTABLE_METHODS = (byte) 0xff;

    private SocksAuthMethod() {
    }
}
