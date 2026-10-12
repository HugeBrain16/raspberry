package org.hugebrain16;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Server {
    private boolean _init = false;
    private ServerSocket _server;
    private ServerSocketFactory _serverfactory;
    private Instance _instance;

    Server(int port, boolean secure) throws IOException {
        if (!secure) {
            _serverfactory = ServerSocketFactory.getDefault();
            this._server = _serverfactory.createServerSocket(port);
            this._init = true;
        } else if (secure && loadKeyStore()) {
            this._server = _serverfactory.createServerSocket(port);
            this._init = true;
        }
    }

    Server(String host, int port, boolean secure) throws IOException {
        if (!secure) {
            _serverfactory = ServerSocketFactory.getDefault();
            this._server = _serverfactory.createServerSocket();
            this._init = true;
        } else if (secure && loadKeyStore()) {
            this._server = _serverfactory.createServerSocket();
            this._init = true;
        }

        if (this._init)
            this._server.bind(new InetSocketAddress(host, port));
    }

    boolean isInit() {
        return _init;
    }

    void setInstance(Instance instance) {
        _instance = instance;
    }

    Instance getInstance() {
        return _instance;
    }

    ServerSocket getServerSocket() {
        if (!isInit())
            return null;

        return _server;
    }

    private boolean loadKeyStore() {
        String path = System.getenv("RASPBERRY_SSL_KEYSTORE");
        if (path == null) return false;

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            System.out.println("SSL: Invalid path to keystore.");
            return false;
        }

        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");

            String password = System.getenv("RASPBERRY_SSL_PASSWORD");
            if (password == null)
                password = "";

            FileInputStream stream = new FileInputStream(file);
            keystore.load(stream, password.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, password.toCharArray());

            SSLContext sslctx = SSLContext.getInstance("TLS");
            sslctx.init(kmf.getKeyManagers(), null, null);
            System.out.println("TLS init OK.");

            _serverfactory = sslctx.getServerSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("SSL: Failed to start server.");
            return false;
        }

        return true;
    }
}
