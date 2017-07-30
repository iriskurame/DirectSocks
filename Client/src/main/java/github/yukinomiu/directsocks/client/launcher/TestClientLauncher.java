package github.yukinomiu.directsocks.client.launcher;

import github.yukinomiu.directsocks.client.core.Client;
import github.yukinomiu.directsocks.client.core.ClientConfig;
import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.common.auth.CachedMD5TokenConverter;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Yukinomiu
 * 2017/7/13
 */
public class TestClientLauncher {
    private static final Logger logger = LoggerFactory.getLogger(TestClientLauncher.class);

    public static void main(String[] args) {
        final String bindAddressName = "localhost";
        final int bindPort = 9090;
        final int backlog = 1000;
        final int workerCount = 3;
        final boolean tcpNoDelay = true;
        final boolean tcpKeepAlive = true;
        final int bufferSize = 1024 * 64;
        final int poolSize = 512;

        final boolean localDnsResolve = true;
        final String serverAddressName = "localhost";
        final int serverPort = 7070;
        final TokenConverter tokenConverter = new CachedMD5TokenConverter();
        final String key = "test";

        // config
        ClientConfig config = new ClientConfig();
        InetAddress bindAddress;
        InetAddress serverAddress;
        try {
            bindAddress = InetAddress.getByName(bindAddressName);
            serverAddress = InetAddress.getByName(serverAddressName);
        } catch (UnknownHostException e) {
            logger.error("地址解析错误", e);
            return;
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

        // start client
        Client client;
        try {
            client = new Client(config);
            client.start();
        } catch (ClientInitException e) {
            logger.error("初始化异常", e);
            return;
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("q")) {
                    break;
                }

                logger.info("unknown client command '{}'", line);
            }

            client.shutdown();
        } catch (IOException e) {
            logger.error("IO异常", e);
        }
    }
}
