package github.yukinomiu.directsocks.server.launcher;

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
        final String localAddressName = "localhost";
        final int localPort = 7070;
        final int backlog = 1000;
        final int workerCount = 3;
        final int bufferSize = 1024;
        final int poolSize = 64;

        // config
        ServerConfig config = new ServerConfig();
        InetAddress localAddress;
        try {
            localAddress = InetAddress.getByName(localAddressName);
        } catch (UnknownHostException e) {
            logger.error("本地绑定地址错误", e);
            return;
        }
        config.setBindAddress(localAddress);
        config.setBindPort(localPort);
        config.setBacklog(backlog);
        config.setWorkerCount(workerCount);
        config.setBufferSize(bufferSize);
        config.setPoolSize(poolSize);

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
