package github.yukinomiu.directsocks.client.launcher;

import github.yukinomiu.directsocks.client.core.Client;
import github.yukinomiu.directsocks.client.core.ClientConfig;
import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.common.auth.CachedMD5TokenConverter;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.common.exception.DirectSocksConfigException;
import github.yukinomiu.directsocks.common.util.PropertiesUtil;
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
public class ConsoleClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleClientLauncher.class);

    public static void main(String[] args) {
        // get config file path
        if (args == null || args.length == 0) {
            logger.error("参数错误");
            return;
        }

        // load config
        logger.info("加载配置文件开始");
        final String configFilePath = args[0];
        final ClientConfig clientConfig;
        try {
            clientConfig = loadClientConfig(configFilePath);
        } catch (DirectSocksConfigException e) {
            logger.error("加载配置文件错误: {}", e.getMessage());
            return;
        }
        logger.info("加载配置文件成功");

        // start client
        logger.info("启动客户端开始");
        Client client;
        try {
            client = new Client(clientConfig);
            client.start();
        } catch (ClientInitException e) {
            logger.error("启动客户端错误", e);
            return;
        }
        logger.info("启动客户端成功");

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

            client.shutdown();
            logger.info("客户端关闭成功");
        } catch (IOException e) {
            logger.error("IO异常", e);
        }
    }

    private static ClientConfig loadClientConfig(String configFilePath) {
        // load config file
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new DirectSocksConfigException("客户端配置文件不存在");
        }
        if (file.isDirectory()) {
            throw new DirectSocksConfigException("客户端配置文件不能是文件夹");
        }

        Properties properties;
        try (InputStream ins = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(ins);
        } catch (FileNotFoundException e) {
            throw new DirectSocksConfigException("客户端配置文件不存在");
        } catch (IOException e) {
            throw new DirectSocksConfigException("加载客户端配置文件时IO错误", e);
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

        final boolean localDnsResolve;
        final String serverAddressName;
        final int serverPort;
        final TokenConverter tokenConverter;
        final String key;

        bindAddressName = PropertiesUtil.getString(properties, "bindAddressName");
        bindPort = PropertiesUtil.getInt(properties, "bindPort");
        backlog = PropertiesUtil.getInt(properties, "backlog");
        workerCount = PropertiesUtil.getInt(properties, "workerCount");
        tcpNoDelay = PropertiesUtil.getBoolean(properties, "tcpNoDelay");
        tcpKeepAlive = PropertiesUtil.getBoolean(properties, "tcpKeepAlive");
        bufferSize = PropertiesUtil.getInt(properties, "bufferSize");
        poolSize = PropertiesUtil.getInt(properties, "poolSize");

        localDnsResolve = PropertiesUtil.getBoolean(properties, "localDnsResolve");
        serverAddressName = PropertiesUtil.getString(properties, "serverAddressName");
        serverPort = PropertiesUtil.getInt(properties, "serverPort");
        tokenConverter = new CachedMD5TokenConverter();
        key = PropertiesUtil.getString(properties, "key");

        // init ClientConfig
        ClientConfig config = new ClientConfig();
        InetAddress bindAddress;
        InetAddress serverAddress;
        try {
            bindAddress = InetAddress.getByName(bindAddressName);
            serverAddress = InetAddress.getByName(serverAddressName);
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

        config.setLocalDnsResolve(localDnsResolve);
        config.setServerAddress(serverAddress);
        config.setServerPort(serverPort);
        config.setTokenConverter(tokenConverter);
        config.setKey(key);
        return config;
    }
}
