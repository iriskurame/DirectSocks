package github.yukinomiu.directsocks.server.launcher;

import github.yukinomiu.directsocks.common.auth.CachedMD5TokenConverter;
import github.yukinomiu.directsocks.common.auth.DefaultTokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenChecker;
import github.yukinomiu.directsocks.common.auth.TokenConverter;
import github.yukinomiu.directsocks.server.core.Server;
import github.yukinomiu.directsocks.server.core.ServerConfig;
import github.yukinomiu.directsocks.server.exception.ServerInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Yukinomiu
 * 2017/7/26
 */
public class TestServerLauncher {
    private static final Logger logger = LoggerFactory.getLogger(TestServerLauncher.class);

    public static void main(String[] args) {
        final String bindAddressName = "localhost";
        final int bindPort = 7070;
        final int backlog = 1000;
        final int workerCount = 3;
        final boolean tcpNoDelay = true;
        final boolean tcpKeepAlive = true;
        final int bufferSize = 1024 * 64;
        final int poolSize = 512;

        final TokenConverter tokenConverter = new CachedMD5TokenConverter();
        final TokenChecker tokenChecker = new DefaultTokenChecker(tokenConverter);

        // config
        ServerConfig config = new ServerConfig();
        InetAddress bindAddress;
        try {
            bindAddress = InetAddress.getByName(bindAddressName);
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

        config.setTokenConverter(tokenConverter);
        config.setTokenChecker(tokenChecker);

        // add keys
        tokenChecker.add("test");

        // start server
        Server server;
        try {
            server = new Server(config);
            server.start();
        } catch (ServerInitException e) {
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

                logger.info("unknown server command '{}'", line);
            }

            server.shutdown();
        } catch (IOException e) {
            logger.error("IO异常", e);
        }
    }
}
