package github.yukinomiu.directsocks.client.launcher;

import github.yukinomiu.directsocks.client.core.Client;
import github.yukinomiu.directsocks.client.core.ClientConfig;
import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.common.auth.CachedMD5TokenGenerator;
import github.yukinomiu.directsocks.common.auth.TokenGenerator;
import github.yukinomiu.directsocks.common.crypto.Crypto;
import github.yukinomiu.directsocks.common.crypto.RC4CRC32Crypto;
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
public final class ConsoleClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleClientLauncher.class);

    public static void main(String[] args) {
        // get client config file path
        final String configFilePath;
        if (args == null || args.length == 0) {
            logger.warn("use default config file name 'client.properties'");
            configFilePath = "client.properties";
        } else {
            configFilePath = args[0];
        }

        // load config
        logger.info("start loading client config file");

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

            control:
            while ((line = bufferedReader.readLine()) != null) {
                logger.info("process command '{}'", line);

                String[] commands = line.split("\\s");
                String command = commands[0];

                switch (command) {
                    case "q":
                        logger.info("client will quit");
                        break control;

                    default:
                        logger.info("unknown command '{}'", line);
                }
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
        String path = file.getAbsolutePath();
        if (!file.exists()) {
            throw new DirectSocksConfigException("client config file '" + path + "' not exists");
        }
        if (file.isDirectory()) {
            throw new DirectSocksConfigException("client config file '" + path + "' can not be directory");
        }

        Properties properties;
        try (InputStream ins = new FileInputStream(file)) {
            properties = new Properties();
            properties.load(ins);
        } catch (FileNotFoundException e) {
            throw new DirectSocksConfigException("client config file '" + path + "' not exists");
        } catch (IOException e) {
            throw new DirectSocksConfigException("loading client config file IO exception", e);
        }

        // get param
        final boolean localDnsResolve;
        final String serverAddressName;
        final int serverPort;
        final TokenGenerator tokenGenerator;
        final String key;
        final String secret;
        final Crypto crypto;

        final String bindAddressName;
        final int bindPort;
        final int backlog;
        final int workerCount;
        final boolean tcpNoDelay;
        final boolean tcpKeepAlive;
        final int readBufferSize;
        final int readPoolSize;
        final int writeBufferSize;
        final int writePoolSize;
        final int frameBufferSize;
        final int framePoolSize;

        localDnsResolve = PropertiesUtil.getBoolean(properties, "localDnsResolve");
        serverAddressName = PropertiesUtil.getString(properties, "serverAddressName");
        serverPort = PropertiesUtil.getInt(properties, "serverPort");
        tokenGenerator = new CachedMD5TokenGenerator();
        key = PropertiesUtil.getString(properties, "key");
        secret = PropertiesUtil.getString(properties, "secret");
        crypto = new RC4CRC32Crypto(secret);

        bindAddressName = PropertiesUtil.getString(properties, "bindAddressName");
        bindPort = PropertiesUtil.getInt(properties, "bindPort");
        backlog = PropertiesUtil.getInt(properties, "backlog");
        workerCount = PropertiesUtil.getInt(properties, "workerCount");
        tcpNoDelay = PropertiesUtil.getBoolean(properties, "tcpNoDelay");
        tcpKeepAlive = PropertiesUtil.getBoolean(properties, "tcpKeepAlive");
        int bufferSize = PropertiesUtil.getInt(properties, "bufferSize");
        int poolSize = PropertiesUtil.getInt(properties, "poolSize");

        readBufferSize = bufferSize;
        readPoolSize = poolSize;
        writeBufferSize = 2 * readBufferSize - 1;
        writePoolSize = poolSize;
        frameBufferSize = bufferSize + crypto.getMaxPadding();
        framePoolSize = poolSize;

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

        config.setLocalDnsResolve(localDnsResolve);
        config.setServerAddress(serverAddress);
        config.setServerPort(serverPort);
        config.setTokenGenerator(tokenGenerator);
        config.setKey(key);
        config.setSecret(secret);
        config.setCrypto(crypto);

        config.setBindAddress(bindAddress);
        config.setBindPort(bindPort);
        config.setBacklog(backlog);
        config.setWorkerCount(workerCount);
        config.setTcpNoDelay(tcpNoDelay);
        config.setTcpKeepAlive(tcpKeepAlive);
        config.setReadBufferSize(readBufferSize);
        config.setReadPoolSize(readPoolSize);
        config.setWriteBufferSize(writeBufferSize);
        config.setWritePoolSize(writePoolSize);
        config.setFrameBufferSize(frameBufferSize);
        config.setFramePoolSize(framePoolSize);

        return config;
    }
}
