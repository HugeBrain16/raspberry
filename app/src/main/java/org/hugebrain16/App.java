package org.hugebrain16;

import java.io.*;
import java.net.*;
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
            String servername = System.getenv("RASPBERRY_SERVER_NAME"); // default: Raspberry chat server
            Server server;
            if (host != null)
                server = new Server(host, port != null ? Integer.parseInt(port) : 5555);
            else
                server = new Server(port != null ? Integer.parseInt(port) : 5555);
            if (servername != null)
                server.name = servername.trim();
            System.out.println(String.format("Listening on %s:%d...",
                server.server.getInetAddress().getHostAddress(), server.server.getLocalPort()));

            while (!server.server.isClosed()) {
                Socket socket = server.server.accept();
                Client client = new Client(socket, server);

                new Incoming(client, server).start();
            }
        } catch(Exception e) {
            System.out.printf("Main thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}
