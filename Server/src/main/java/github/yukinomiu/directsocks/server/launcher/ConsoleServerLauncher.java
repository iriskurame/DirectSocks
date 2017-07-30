package github.yukinomiu.directsocks.server.launcher;

import github.yukinomiu.directsocks.common.auth.CachedMD5TokenConverter;
import github.yukinomiu.directsocks.common.auth.DefaultTokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.common.exception.DirectSocksConfigException;
import github.yukinomiu.directsocks.common.util.PropertiesUtil;
import github.yukinomiu.directsocks.server.core.Server;
import github.yukinomiu.directsocks.server.core.ServerConfig;
import github.yukinomiu.directsocks.server.exception.ServerInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Yukinomiu
 * 2017/7/30
 */
public class ConsoleServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleServerLauncher.class);

    public static void main(String[] args) {
        // get config file path
        if (args == null || args.length == 0) {
            logger.error("参数错误");
            return;
        }

        // load config
        logger.info("加载配置文件开始");
        final String configFilePath = args[0];
        final ServerConfig serverConfig;
        try {
            serverConfig = loadServerConfig(configFilePath);
        } catch (DirectSocksConfigException e) {
            logger.error("加载配置文件错误: {}", e.getMessage());
            return;
        }
        logger.info("加载配置文件成功");

        // start client
        logger.info("启动服务端开始");
        Server server;
        try {
            server = new Server(serverConfig);
            server.start();
        } catch (ServerInitException e) {
            logger.error("启动服务端错误", e);
            return;
        }
        logger.info("启动服务端成功");

        // wait command
        logger.info("请输入命令, 按回车键提交\r\n");
        try (InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("q")) {
                    break;
                }

                logger.info("未知命令: {}", line);
            }

            server.shutdown();
            logger.info("服务端关闭成功");
        } catch (IOException e) {
            logger.error("IO异常", e);
        }
    }

    private static ServerConfig loadServerConfig(String configFilePath) {
        // load config file
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new DirectSocksConfigException("服务端配置文件不存在");
        }
        if (file.isDirectory()) {
            throw new DirectSocksConfigException("服务端配置文件不能是文件夹");
        }

        Properties properties;
        try (InputStream ins = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(ins);
        } catch (FileNotFoundException e) {
            throw new DirectSocksConfigException("服务端配置文件不存在");
        } catch (IOException e) {
            throw new DirectSocksConfigException("加载服务端配置文件时IO错误", e);
        }

        // get param
        final String bindAddressName;
        final int bindPort;
        final int backlog;
        final int workerCount;
        final boolean tcpNoDelay;
        final boolean tcpKeepAlive;
        final int bufferSize;
        final int poolSize;

        final TokenConverter tokenConverter;
        final TokenChecker tokenChecker;
        final String keys;

        bindAddressName = PropertiesUtil.getString(properties, "bindAddressName");
        bindPort = PropertiesUtil.getInt(properties, "bindPort");
        backlog = PropertiesUtil.getInt(properties, "backlog");
        workerCount = PropertiesUtil.getInt(properties, "workerCount");
        tcpNoDelay = PropertiesUtil.getBoolean(properties, "tcpNoDelay");
        tcpKeepAlive = PropertiesUtil.getBoolean(properties, "tcpKeepAlive");
        bufferSize = PropertiesUtil.getInt(properties, "bufferSize");
        poolSize = PropertiesUtil.getInt(properties, "poolSize");

        tokenConverter = new CachedMD5TokenConverter();
        tokenChecker = new DefaultTokenChecker(tokenConverter);
        keys = PropertiesUtil.getString(properties, "keys");

        // init ClientConfig
        ServerConfig config = new ServerConfig();
        InetAddress bindAddress;
        try {
            bindAddress = InetAddress.getByName(bindAddressName);
        } catch (UnknownHostException e) {
            throw new DirectSocksConfigException("地址解析错误", e);
        }
        config.setBindAddress(bindAddress);
        config.setBindPort(bindPort);
        config.setBacklog(backlog);
        config.setWorkerCount(workerCount);
        config.setTcpNoDelay(tcpNoDelay);
        config.setTcpKeepAlive(tcpKeepAlive);
        config.setBufferSize(bufferSize);
        config.setPoolSize(poolSize);

        config.setTokenConverter(tokenConverter);
        config.setTokenChecker(tokenChecker);

        String[] keysArray = keys.split(",");
        for (String key : keysArray) {
            key = key.trim();
            tokenChecker.add(key);
        }
        return config;
    }
}
