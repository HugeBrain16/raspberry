package org.hugebrain16;

import java.net.ServerSocket;
import java.net.Socket;

public class Listener extends Thread {
    Server server;

    Listener(Server server) {
        this.server = server;
    }

    public void run() {
        try {
            ServerSocket serversocket = server.getServerSocket();
            while (serversocket != null && !serversocket.isClosed()) {
                Socket socket = serversocket.accept();
                Client client = new Client(socket, server);

                new Incoming(client, server).start();
            }
        } catch(Exception e) {
            System.out.printf("Listener thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}
