package github.yukinomiu.directsocks.common.util;

import github.yukinomiu.directsocks.common.exception.DirectSocksConfigException;

import java.util.Properties;

/**
 * Yukinomiu
 * 2017/7/30
 */
public class PropertiesUtil {
    private PropertiesUtil() {
    }

    public static String getString(final Properties properties, final String name) {
        String value = properties.getProperty(name);
        if (value == null || value.length() == 0) throw new DirectSocksConfigException("配置项为空: " + name);
        return value;
    }

    public static int getInt(final Properties properties, final String name) {
        String value = properties.getProperty(name);
        if (value == null || value.length() == 0) throw new DirectSocksConfigException("配置项为空: " + name);

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            throw new DirectSocksConfigException("配置项必须为数字: " + name);
        }
    }

    public static boolean getBoolean(final Properties properties, final String name) {
        String value = properties.getProperty(name);
        if (value == null || value.length() == 0) throw new DirectSocksConfigException("配置项为空: " + name);

        if ("true".equals(value.toLowerCase())) {
            return true;
        } else if ("false".equals(value.toLowerCase())) {
            return false;
        } else {
            throw new DirectSocksConfigException("配置项必须为布尔值: " + name);
        }
    }
}
