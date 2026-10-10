package org.hugebrain16;

import java.io.BufferedReader;
import java.io.PrintWriter;

import io.github.kamilszewc.javaansitextcolorizer.Colorizer;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer.Color;

public class Incoming extends Thread {
    Client client;
    Server server;

    Incoming(Client client, Server server) {
        this.client = client;
        this.server = server;
    }

    public void run() {
        String channel = System.getenv("RASPBERRY_DEFAULT_CHANNEL"); // default: general
        int maxUsername;
        try {
            maxUsername = Integer.parseInt(System.getenv("RASPBERRY_MAX_USERNAME_LENGTH")); // default: 16
        } catch(NumberFormatException e) {
            maxUsername = 16;
        }

        try {
            boolean nameCheck = false;
            boolean nameValid = false;
            boolean nameLength = false;
            boolean rconPass = false;
            BufferedReader streamIn = client.getReader();
            PrintWriter streamOut = client.getWriter();
            while (client.username == null) {
                client.clearScreen(streamOut);
                if (nameValid) {
                    streamOut.write(Colorizer.color("Invalid username format!\n\n", Color.RED));    
                    nameValid = false;
                }
                
                if (nameCheck) {
                    streamOut.write(Colorizer.color("Username already in use!\n\n", Color.RED));    
                    nameCheck = false;
                } 

                if (nameLength) {
                    streamOut.write(Colorizer.color(String.format("Username is too long! max: %d characters\n\n", maxUsername), Color.RED));
                    nameLength = false;
                }

                if (rconPass) {
                    streamOut.write(Colorizer.color("Wrong password!\n\n", Color.RED));
                    rconPass = false;
                }

                streamOut.write(String.format("%s (Raspberry v%s)\nEnter username: ", server.name != null ? server.name : "Raspberry chat server", App.getVersion()));
                streamOut.flush();

                String username = streamIn.readLine();
                if (username == null)
                    break;
                username = username.trim();

                if (!username.matches("^[a-zA-Z0-9_\\-\\.]+$")) {
                    nameValid = true;
                    continue;
                }

                if (username.length() > maxUsername) {
                    nameLength = true;
                    continue;
                }

                if (!username.isEmpty() && !server.checkUsername(username)) {
                    client.username = username;

                    if (channel != null)
                        client.channel = channel;
                    else
                        client.channel = "general";
                    break;
                }

                nameCheck = true;
            }

            if (client.username != null) {
                if (client.username.equalsIgnoreCase("RCON")) {
                    String password = System.getenv("RASPBERRY_RCON_PASSWORD");
                    if (password == null) {
                        streamOut.write("Can't connect to RCON.\n");
                        streamOut.flush();
                        client.socket.close();
                    }

                    streamOut.write("Enter password: ");
                    streamOut.flush();

                    String pass = streamIn.readLine();
                    if (password.equals(pass)) {
                        RCON rcon = new RCON(client.socket, server);
                        rcon.start();
                    } else {
                        streamOut.write("Wrong password.\n");
                        streamOut.flush();
                        client.socket.close();
                    }
                } else {
                    server.clients.add(client);
                    client.start();
                }
            } else
                client.socket.close();
        } catch(Exception e) {
            System.out.printf("Incoming thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}
