package org.hugebrain16;

import java.io.*;
import java.util.Properties;

public class App extends Thread {

    public static String getVersion() throws IOException {
        Properties props = new Properties();
        props.load(App.class.getResourceAsStream("/raspberry.properties"));

        return props.getProperty("version");
    }

    public static void main(String[] args) {
        try {
            String port = System.getenv("RASPBERRY_PORT"); // default: 5555
            String host = System.getenv("RASPBERRY_HOST"); // default: 0.0.0.0 (any)
            String sslport = System.getenv("RASPBERRY_SSL_PORT"); // default: RASPBERRY_PORT + 1
            String servername = System.getenv("RASPBERRY_SERVER_NAME"); // default: Raspberry chat server
            boolean sslonly = Boolean.parseBoolean(System.getenv("RASPBERRY_SSL_ONLY"));

            Server server = null;
            Server sslserver;
            if (host != null) {
                int _port = port != null ? Integer.parseInt(port) : 5555;

                if (!sslonly)
                    server = new Server(host, _port, false);
                sslserver = new Server(host, sslport != null ? Integer.parseInt(sslport) : _port + 1, true);
            } else {
                int _port = port != null ? Integer.parseInt(port) : 5555;

                if (!sslonly)
                    server = new Server(port != null ? Integer.parseInt(port) : 5555, false);
                sslserver = new Server(sslport != null ? Integer.parseInt(sslport) : _port + 1, true);
            }

            Instance instance = new Instance();
            if (servername != null)
                instance.name = servername.trim();

            if (!sslonly)
                server.setInstance(instance);
            sslserver.setInstance(instance);

            if (!sslonly && server.isInit())
                System.out.println(String.format("Listening on %s:%d...",
                    server.getServerSocket().getInetAddress().getHostAddress(), server.getServerSocket().getLocalPort()));
            if (sslserver.isInit())
                System.out.println(String.format("SSL Listening on %s:%d...",
                    sslserver.getServerSocket().getInetAddress().getHostAddress(), sslserver.getServerSocket().getLocalPort()));

            if (!sslonly && server.isInit())
                new Listener(server).start();
            if (sslserver.isInit())
                new Listener(sslserver).start();
        } catch(Exception e) {
            System.out.printf("Main thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}
