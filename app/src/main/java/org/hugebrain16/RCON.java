package org.hugebrain16;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RCON extends Thread {
    Socket socket;
    Server server;
    private BufferedReader _reader = null;
    private PrintWriter _writer = null;

    RCON(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    BufferedReader getReader() throws IOException {
        if (this._reader == null)
            this._reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        return this._reader; 
    }

    PrintWriter getWriter() throws IOException {
        if (this._writer == null)
            this._writer = new PrintWriter(socket.getOutputStream());
    
        return this._writer;
    }

    public void run() {
        try {
            BufferedReader streamIn = getReader();
            PrintWriter streamOut = getWriter();

            streamOut.write("RCON Ready! type /help for list of commands.\n");
            streamOut.flush();
            while (!server.server.isClosed()) {
                streamOut.write("> ");
                streamOut.flush();
                String message = streamIn.readLine();

                if (message == null)
                    break;

                message = message.trim();
                if (message.isEmpty())
                    continue;

                if (message.startsWith("/")) {
                    String[] cmd = message.split(" ");

                    if (cmd[0].equals("/shutdown")) {
                        streamOut.write("Shutting down...\n");
                        streamOut.flush();
                        server.server.close();
                    } else if (cmd[0].equals("/channels")) {
                        if (server.channels.isEmpty()) {
                            streamOut.write("No active channels.\n");
                            streamOut.flush();
                            continue;
                        }

                        streamOut.write("List of active channels:\n");
                        server.channels.forEach((key, val) -> {
                            String line = "- " + key;
                            line += " (";

                            line += "users: " + server.getChannelPeerCount(key) + ", ";
                            line += "messages: " + val.size();

                            line += ")\n";
                            streamOut.write(line);
                        });
                        streamOut.flush();
                    } else if (cmd[0].equals("/users")) {
                        if (server.clients.isEmpty()) {
                            streamOut.write("No active users.\n");
                            streamOut.flush();
                            continue;
                        }

                        streamOut.write("List of active users:\n");
                        for (Client client : server.clients) {
                            String line = "- " + Util.paint(client.username, client.color);
                            line += " (";

                            line += "channel: " + client.channel;

                            line += ")\n";
                            streamOut.write(line);
                        }
                        streamOut.flush();
                    } else if (cmd[0].equals("/quit")) {
                        socket.close();
                    } else {
                        streamOut.write("Unknown command: " + cmd[0] + "\n");
                        streamOut.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.out.printf("RCON thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}
