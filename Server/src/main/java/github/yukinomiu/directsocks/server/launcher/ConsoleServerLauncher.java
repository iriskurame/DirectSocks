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
        // get server config file path
        if (args == null || args.length == 0) {
            logger.error("bad argument");
            return;
        }

        // load config
        logger.info("start loading server config file");
        final String configFilePath = args[0];
        final ServerConfig serverConfig;
        try {
            serverConfig = loadServerConfig(configFilePath);
        } catch (DirectSocksConfigException e) {
            logger.error("loading server config file exception: {}", e.getMessage());
            return;
        }
        logger.info("loading server config file done");

        // start client
        logger.info("start server");
        Server server;
        try {
            server = new Server(serverConfig);
            server.start();
        } catch (ServerInitException e) {
            logger.error("start server exception: {}", e.getMessage());
            return;
        }
        logger.info("start server done");

        // wait command
        logger.info("please enter command and press 'Enter' to submit\r\n");
        try (InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            TokenChecker tokenChecker = serverConfig.getTokenChecker();
            label:
            while ((line = bufferedReader.readLine()) != null) {
                logger.info("process command '{}'", line);

                String[] commands = line.split("\\s");
                String command = commands[0];

                switch (command) {
                    case "q":
                        logger.info("server will quit");
                        break label;

                    case "add":
                        if (commands.length > 1) {
                            for (int i = 1; i < commands.length; i++) {
                                String key = commands[i];
                                tokenChecker.add(key);
                                logger.info("add key '{}'", key);
                            }
                        }
                        break;

                    case "remove":
                        if (commands.length > 1) {
                            for (int i = 1; i < commands.length; i++) {
                                String key = commands[i];
                                boolean isRemoved = tokenChecker.remove(key);
                                if (isRemoved) {
                                    logger.info("key '{}' removed", key);
                                } else {
                                    logger.info("key '{}' not exists", key);
                                }
                            }
                        }
                        break;

                    case "list":
                        for (String key : tokenChecker.listKeys()) {
                            logger.info("'{}'", key);
                        }
                        break;

                    default:
                        logger.info("unknown command '{}'", line);
                }
            }

            server.shutdown();
            logger.info("server closed");
        } catch (IOException e) {
            logger.error("IO exception: {}", e.getMessage());
        }
    }

    private static ServerConfig loadServerConfig(String configFilePath) {
        // load server config file
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new DirectSocksConfigException("server config file not exists");
        }
        if (file.isDirectory()) {
            throw new DirectSocksConfigException("server config file can not be directory");
        }

        Properties properties;
        try (InputStream ins = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(ins);
        } catch (FileNotFoundException e) {
            throw new DirectSocksConfigException("server config file not exists");
        } catch (IOException e) {
            throw new DirectSocksConfigException("loading server config file IO exception", e);
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
            throw new DirectSocksConfigException("resolving host address exception", e);
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