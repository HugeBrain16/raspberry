package org.hugebrain16;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.kamilszewc.javaansitextcolorizer.Colorizer.BackgroundColor;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer.Color;

enum Mode {
    CHAT,
    WHISPER
}

public class Client extends Thread {
    Socket socket;
    Server server;
    String username = null;
    String channel = null;
    BackgroundColor color = Util.randomBackgroundColor();
    String cmdMessage = null;
    Color cmdMessageColor = null;
    Mode mode = Mode.CHAT;
    String whisper = null;
    boolean showTimestamp = false;
    long lastMessage = System.currentTimeMillis();
    ConcurrentHashMap<String, CopyOnWriteArrayList<Message>> channels = new ConcurrentHashMap<String, CopyOnWriteArrayList<Message>>();
    private BufferedReader _reader = null;
    private PrintWriter _writer = null;

    Client(Socket socket, Server server) {
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

    void clearScreen(PrintWriter streamOut) {
        streamOut.write("\033[2J\033[H");
        streamOut.flush();
    }

    void sendCmdMessage(String message, Color color) {
        cmdMessage = message;
        cmdMessageColor = color;
        server.syncClient(this, channel);
    }

    void updateMessage(String sender, String content) {
        int maxBuffer;
        try {
            maxBuffer = Integer.parseInt(System.getenv("RASPBERRY_MESSAGE_FREE_THRESHOLD")); // default: 50
        } catch(NumberFormatException e) {
            maxBuffer = 50;
        }

        if (!channels.containsKey(channel))
            channels.put(channel, new CopyOnWriteArrayList<Message>());

        CopyOnWriteArrayList<Message> message = channels.get(channel);
        if (message.size() > maxBuffer)
            message.remove(0);

        message.add(new Message(sender, content));
        server.syncClient(this, channel);
    }

    String getModeString() {
        String mode = null;

        if (this.mode == Mode.WHISPER && whisper != null)
            mode = String.format("[whisper (to: %s)]", whisper);

        return mode;
    }

    public void run() {
        try {
            int maxMessage;
            long maxRate;
            try {
                maxMessage = Integer.parseInt(System.getenv("RASPBERRY_MAX_MESSAGE_LENGTH")); // default: 256
                maxRate = Long.parseLong(System.getenv("RASPBERRY_MAX_MESSAGE_RATE")); // default: 100ms
            } catch(NumberFormatException e) {
                maxMessage = 256;
                maxRate = 100;
            }

            String motd = server.readMotd();
            if (motd != null)
                updateMessage("MOTD", motd);
            server.updateMessage(channel, "Server", String.format("%s has joined the chat", username));
            updateMessage("Server", "type /help to see all available commands");

            while (!server.server.isClosed()) {
                BufferedReader streamIn = getReader();

                String message = streamIn.readLine();
                long now = System.currentTimeMillis();
                if (now - lastMessage < maxRate) {
                    sendCmdMessage("Slow down!", Color.RED);
                    continue;
                }
                lastMessage = now;

                if (message == null)
                    break;

                if (!message.trim().isEmpty()) {
                    message = message.trim();

                    if (message.length() > maxMessage) {
                        message = message.substring(0, maxMessage);
                    }

                    if (message.startsWith("/")) {
                        String[] cmd = message.split(" ");

                        if (cmd[0].equals("/join")) {
                            if (cmd.length > 1) {
                                if (cmd[1].matches("^[a-zA-Z0-9_\\-\\.]+$")) {
                                    server.updateMessage(channel, "Server", String.format("%s has left the chat", username));
                                    channel = cmd[1];
                                    server.updateMessage(channel, "Server", String.format("%s has joined the chat", username));
                                } else {
                                    sendCmdMessage("Invalid channel name", Color.RED);
                                }
                            } else
                                sendCmdMessage("Usage: /join <channel>", Color.YELLOW);
                        } else if (cmd[0].equals("/quit")) {
                            break;
                        } else if (cmd[0].equals("/timestamp")) {
                            showTimestamp = !showTimestamp;
                            server.syncClient(this, channel);
                        } else if (cmd[0].equals("/refresh")) {
                            sendCmdMessage("", Color.YELLOW);
                        } else if (cmd[0].equals("/help")) {
                            String help = "List available commands:\n";
                            help += "- /help\n";
                            help += "\tlist available commands\n";
                            help += "- /users\n";
                            help += "\tlist users in the current channel\n";
                            help += "- /whisper\n";
                            help += "\twhisper to other user\n";
                            help += "- /join\n";
                            help += "\tjoin a channel\n";
                            help += "- /color\n";
                            help += "\tchange name color\n";
                            help += "- /timestamp\n";
                            help += "\ttoggle messages timestamp\n";
                            help += "- /refresh\n";
                            help += "\trefresh the buffer (use if text glitches occur)\n";
                            help += "- /quit\n";
                            help += "\tleave chat and disconnect\n";
                            sendCmdMessage(help, Color.GREEN);
                        } else if (cmd[0].equals("/users")) {
                            String users = "Users in this channel:\n";
                            
                            for (Client client : server.clients) {
                                if (client.channel.equals(channel))
                                    users = users + "- " + client.username + "\n";
                            }

                            sendCmdMessage(users, Color.GREEN);
                        } else if (cmd[0].equals("/whisper")) {
                            if (cmd.length > 1) {
                                Client client = server.getClientByUsername(cmd[1], channel);

                                if (client == this) {
                                    sendCmdMessage("Can't whisper to self", Color.YELLOW);
                                } else if (client != null) {
                                    mode = Mode.WHISPER;
                                    whisper = client.username;
                                    sendCmdMessage("'/whisper' to disable", Color.YELLOW);
                                } else
                                    sendCmdMessage("User not found! make sure you are in the same channel", Color.YELLOW);
                            } else {
                                if (mode == Mode.WHISPER) {
                                    whisper = null;
                                    mode = Mode.CHAT;
                                    sendCmdMessage("", Color.YELLOW); // force sync
                                } else
                                    sendCmdMessage("Usage: /whisper <user>", Color.YELLOW);
                            }
                        } else if (cmd[0].equals("/color")) {
                            if (cmd.length > 1) {
                                BackgroundColor prevColor = color;

                                if (cmd[1].equals("random"))
                                    color = Util.randomBackgroundColor();
                                else if (cmd[1].equals("list")) {
                                    String list = "";
                                    for (int i = 0; i < Util.BackgroundColors.size(); i++) {
                                        String key = new Vector<>(Util.BackgroundColors.keySet()).get(i);
                                        BackgroundColor col = new Vector<>(Util.BackgroundColors.values()).get(i);

                                        list += "- " + Util.paint(key, col) + "\n";
                                    }
                                    sendCmdMessage(list, Color.WHITE);
                                } else {
                                    BackgroundColor newColor = Util.BackgroundColors.get(cmd[1]);
                                    if (newColor == null)
                                        sendCmdMessage("Unknown color: " + cmd[1], Color.RED);
                                    else
                                        color = newColor;
                                }

                                if (prevColor != color)
                                    sendCmdMessage("Successfully switched color", Color.YELLOW);
                            } else
                                sendCmdMessage("Usage: /color <color>", Color.YELLOW);
                        } else
                            sendCmdMessage("Unknown command: " + cmd[0], Color.YELLOW);
                    } else {
                        if (mode == Mode.CHAT) {
                            server.updateMessage(channel, Util.paint(username, color), message);
                        } else if (mode == Mode.WHISPER) {
                            Client target = server.getClientByUsername(whisper, channel);
                            if (target != null) {
                                updateMessage(String.format("[whisper] %s", Util.paint(username, color)), message);
                                target.updateMessage(String.format("[whisper] %s", Util.paint(username, color)), message);
                            } else {
                                sendCmdMessage("Can't no longer reach the target, whisper disabled.", Color.YELLOW);
                                mode = Mode.CHAT;
                                whisper = null;
                            }
                        }

                        cmdMessage = null;
                        cmdMessageColor = null;
                        sendCmdMessage("", Color.YELLOW);
                    }
                }
            }

            server.updateMessage(channel, "Server", String.format("%s has left the chat", username));
            socket.close();

            server.clients.remove(this);
        } catch(Exception e) {
            System.out.printf("Client thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        } finally {
            server.clients.remove(this);
            try {
                socket.close();
            } catch (Exception e) {}
            server.updateMessage(channel, "Server", String.format("%s has left the chat", username));
        }
    }
}
