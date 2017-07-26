package github.yukinomiu.directsocks.client.launcher;

import github.yukinomiu.directsocks.client.exception.ClientInitException;
import github.yukinomiu.directsocks.client.server.Client;
import github.yukinomiu.directsocks.client.server.ClientConfig;
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
public class TestLauncher {
    private static final Logger logger = LoggerFactory.getLogger(TestLauncher.class);

    public static void main(String[] args) {
        final String localAddressName = "localhost";
        final int localPort = 9090;
        final int backlog = 1000;
        final int workerCount = 3;
        final int bufferSize = 1024;
        final int poolSize = 64;
        final boolean localDnsResolve = true;

        // config
        ClientConfig config = new ClientConfig();
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
        config.setLocalDnsResolve(localDnsResolve);

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

                logger.info("unknown command '{}'", line);
            }

            client.shutdown();
        } catch (IOException e) {
            logger.error("IO异常", e);
        }

    }
}
