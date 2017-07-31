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
        // get client config file path
        if (args == null || args.length == 0) {
            logger.error("bad argument");
            return;
        }

        // load config
        logger.info("start loading client config file");
        final String configFilePath = args[0];
        final ClientConfig clientConfig;
        try {
            clientConfig = loadClientConfig(configFilePath);
        } catch (DirectSocksConfigException e) {
            logger.error("loading client config file exception: {}", e.getMessage());
            return;
        }
        logger.info("loading client config file done");

        // start client
        logger.info("start client");
        Client client;
        try {
            client = new Client(clientConfig);
            client.start();
        } catch (ClientInitException e) {
            logger.error("start client exception: {}", e.getMessage());
            return;
        }
        logger.info("start client done");

        // wait command
        logger.info("please enter command and press 'Enter' to submit\r\n");
        try (InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("q")) {
                    break;
                }

                logger.info("unknown command '{}'", line);
            }

            client.shutdown();
            logger.info("client closed");
        } catch (IOException e) {
            logger.error("IO exception: {}", e.getMessage());
        }
    }

    private static ClientConfig loadClientConfig(String configFilePath) {
        // load client config file
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new DirectSocksConfigException("client config file not exists");
        }
        if (file.isDirectory()) {
            throw new DirectSocksConfigException("client config file can not be directory");
        }

        Properties properties;
        try (InputStream ins = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(ins);
        } catch (FileNotFoundException e) {
            throw new DirectSocksConfigException("client config file not exists");
        } catch (IOException e) {
            throw new DirectSocksConfigException("loading client config file IO exception", e);
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
            throw new DirectSocksConfigException("resolving host address exception");
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
