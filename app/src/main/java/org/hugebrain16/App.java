package org.hugebrain16;

import java.io.*;
import java.net.*;
import java.util.regex.Pattern;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer.*;

enum Mode {
    CHAT,
    WHISPER
}

class Client extends Thread {
    Socket socket;
    Server server;
    String username = null;
    String channel = null;
    BackgroundColor color = BackgroundColor.GREEN;
    String cmdMessage = null;
    Color cmdMessageColor = null;
    Mode mode = Mode.CHAT;
    String whisper = null;
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
        cmdMessage = null;
        cmdMessageColor = null;
    }

    void updateMessage(String sender, String content) {
        int maxBuffer;
        try {
            maxBuffer = Integer.parseInt(System.getenv("RASPBERRY_MESSAGE_FREE_THRESHOLD")); // default: 50
        } catch(NumberFormatException e) {
            maxBuffer = 50;
        }

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
                        } else if (cmd[0].equals("/help")) {
                            String help = "List available commands:\n";
                            help = help + "- /help\n";
                            help = help + "\tlist available commands\n";
                            help = help + "- /users\n";
                            help = help + "\tlist users in the current channel\n";
                            help = help + "- /whisper\n";
                            help = help + "\twhisper to other user\n";
                            help = help + "- /join\n";
                            help = help + "\tjoin a channel\n";
                            help = help + "- /color\n";
                            help = help + "\tchange name color\n";
                            help = help + "- /quit\n";
                            help = help + "\tleave chat and disconnect\n";
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
                                if (whisper != null && mode == Mode.WHISPER) {
                                    whisper = null;
                                    mode = Mode.CHAT;
                                } else
                                    sendCmdMessage("Usage: /whisper <user>", Color.YELLOW);
                            }
                        } else if (cmd[0].equals("/color")) {
                            if (cmd.length > 1) {
                                BackgroundColor prevColor = color;

                                switch (cmd[1].toLowerCase()) {
                                    case "green":
                                        color = BackgroundColor.GREEN;
                                        break;
                                    case "yellow":
                                        color = BackgroundColor.YELLOW;
                                        break;
                                    case "cyan":
                                        color = BackgroundColor.CYAN;
                                        break;
                                    case "magenta":
                                        color = BackgroundColor.MAGENTA;
                                        break;
                                    case "red":
                                        color = BackgroundColor.RED;
                                        break;
                                    case "blue":
                                        color = BackgroundColor.BLUE;
                                        break;
                                    case "white":
                                        color = BackgroundColor.WHITE;
                                        break;
                                    default:
                                        sendCmdMessage("Unknown color: " + cmd[1], Color.RED);
                                        break;
                                }

                                if (prevColor != color)
                                    sendCmdMessage("Successfully switched color", Color.YELLOW);
                            } else
                                sendCmdMessage("Usage: /color <color>", Color.YELLOW);
                        } else
                            sendCmdMessage("Unknown command: " + cmd[0], Color.YELLOW);
                    } else {
                        if (mode == Mode.CHAT) {
                            server.updateMessage(channel, Colorizer.color(username, color), message);
                        } else if (mode == Mode.WHISPER) {
                            Client target = server.getClientByUsername(whisper, channel);

                            updateMessage(String.format("[whisper] %s", Colorizer.color(username, color)), message);
                            target.updateMessage(String.format("[whisper] %s", Colorizer.color(username, color)), message);
                        }
                    }
                }
            }

            server.updateMessage(channel, "Server", String.format("%s has left the chat", username));
            socket.close();

            server.clients.remove(this);
        } catch(Exception e) {
            System.out.printf("Client thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}

class Message {
    String sender;
    String content;
    
    Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    Message(Message message) {
        this.sender = message.sender;
        this.content = message.content;
    }
}

class Server {
    ServerSocket server;
    String name;
    ConcurrentHashMap<String, CopyOnWriteArrayList<Message>> channels = new ConcurrentHashMap<String, CopyOnWriteArrayList<Message>>();
    Vector<Client> clients = new Vector<Client>();

    Server(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    Server(String host, int port) throws IOException {
        this.server = new ServerSocket(port, 256, InetAddress.getByName(host));
    }

    int getChannelPeerCount(String channel) {
        int count = 0;

        for (Client client : clients) {
            if (client.channel.equals(channel))
                count++;
        }

        return count;
    }
    
    Client getClientByUsername(String username, String channel) {
        for (Client client : clients) {
            if (client.username.equals(username) && client.channel.equals(channel))
                return client;
        }

        return null;
    }

    CopyOnWriteArrayList<Message> cloneMessages(String channel) {
        CopyOnWriteArrayList<Message> clone = new CopyOnWriteArrayList<Message>();
        for (Message message : channels.get(channel)) {
            clone.add(new Message(message));
        }

        return clone;
    }

    void updateMessage(String channel, String sender, String content) {
        int maxBuffer;
        try {
            maxBuffer = Integer.parseInt(System.getenv("RASPBERRY_MESSAGE_FREE_THRESHOLD")); // default: 50
        } catch(NumberFormatException e) {
            maxBuffer = 50;
        }
        
        if (!channels.containsKey(channel))
            channels.put(channel, new CopyOnWriteArrayList<Message>());

        CopyOnWriteArrayList<Message> message = this.channels.get(channel);
        if (message.size() > maxBuffer)
            message.remove(0);

        message.add(new Message(sender, content));
        for (Client client : clients) {
            if (!client.channels.containsKey(channel))
                client.channels.put(channel, cloneMessages(channel));
            else {
                message = client.channels.get(channel);
                if (message.size() > maxBuffer)
                    message.remove(0);
                
                message.add(new Message(sender, content));
            }
            syncClient(client, channel);
        }
    }

    boolean checkUsername(String username) {
        for (Client client : clients) {
            if (client.username.equals(username))
                return true;
        }

        if (username.equalsIgnoreCase("server"))
            return true;

        return false;
    }

    void syncClient(Client client, String channel) {
        if (!client.channel.equals(channel))
            return;

        String messages = "";

        for (Message message : client.channels.get(channel)) {
            message.content = message.content.replaceAll(
                "(?<!\\S)@" + Pattern.quote(client.username) + "(?=\\b|[.,!?]|$)",
                Colorizer.color("@" + client.username, Color.YELLOW));

            if (message.sender.equals("Server"))
                messages = messages + Colorizer.color(message.content + "\n", Color.YELLOW);
            else
                messages = messages + String.format("%s: %s\n", message.sender, message.content);
        }
        messages = messages + String.format("\n\n\n%s%s(%s) [#%s (online: %s)]\nType your message: ",
            client.cmdMessage != null ? client.cmdMessageColor != null ? Colorizer.color(client.cmdMessage + "\n", client.cmdMessageColor) : client.cmdMessage + "\n" : "",
            client.getModeString() != null ? client.getModeString() : "",
            Colorizer.color(client.username, client.color), channel,
            Colorizer.color(Integer.toString(getChannelPeerCount(channel)), Color.GREEN));
            
        try {
            PrintWriter streamOut = client.getWriter();
            client.clearScreen(streamOut);
            streamOut.write(messages);
            streamOut.flush();
        } catch(Exception e) {
            System.out.printf("Server thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}

class Incoming extends Thread {
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
            while (client.username == null) {
                PrintWriter streamOut = client.getWriter();
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

                streamOut.write(String.format("%s (Raspberry v%s)\nEnter username: ", server.name != null ? server.name : "Raspberry chat server", App.version));
                streamOut.flush();

                BufferedReader streamIn = client.getReader();
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
                server.clients.add(client);
                client.start();
            } else
                client.socket.close();
        } catch(Exception e) {
            System.out.printf("Incoming thread error: %s\n", e.getMessage() != null ? e.getMessage() : e);
        }
    }
}

public class App extends Thread {
    public static String version = "0.1.0";

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
