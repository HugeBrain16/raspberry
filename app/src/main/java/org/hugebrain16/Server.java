package org.hugebrain16;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.format.DateTimeFormatter;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import io.github.kamilszewc.javaansitextcolorizer.Colorizer;
import io.github.kamilszewc.javaansitextcolorizer.Colorizer.Color;

public class Server extends Thread {
    ServerSocket server;
    String name;
    ConcurrentHashMap<String, CopyOnWriteArrayList<Message>> channels = new ConcurrentHashMap<String, CopyOnWriteArrayList<Message>>();
    Vector<Client> clients = new Vector<Client>();
    private DateTimeFormatter _ftime;
    private String _motd = System.getenv("RASPBERRY_MOTD");

    Server(int port) throws IOException {
        this.server = new ServerSocket(port);
        this.initTimestamp();
    }

    Server(String host, int port) throws IOException {
        this.server = new ServerSocket(port, 256, InetAddress.getByName(host));
        this.initTimestamp();
    }

    public String readMotd() {
        if (_motd == null)
            return null;

        try {
            File motd = new File(_motd);
            if (!motd.exists())
                motd.createNewFile();
            if (!motd.isFile())
                return null;
            if (!motd.setReadable(true))
                return null;

            FileInputStream stream = new FileInputStream(motd);
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            int b;
            while ((b = stream.read()) != -1)
                content.write(b);
            stream.close();

            return content.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initTimestamp() {
        String ftime = System.getenv("RASPBERRY_TIMESTAMP_FORMAT"); // default: dd-MM-yyyy HH:mm:ss
        if (ftime == null)
            _ftime = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        else
            _ftime = DateTimeFormatter.ofPattern(ftime);
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

        if (username.equalsIgnoreCase("server") || username.equalsIgnoreCase("motd"))
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
            else if (message.sender.equals("MOTD"))
                messages = messages + message.content + "\n";
            else
                messages = messages + String.format("%s%s: %s\n", client.showTimestamp ? Colorizer.color(message.time.format(_ftime) + " ", Color.BLACK_BRIGHT) : "", message.sender, message.content);
        }
        messages = messages + String.format("\n\n\n%s%s(%s) [#%s (online: %s)]\nType your message: ",
            client.cmdMessage != null ? client.cmdMessageColor != null ? Colorizer.color(client.cmdMessage + "\n", client.cmdMessageColor) : client.cmdMessage + "\n" : "",
            client.getModeString() != null ? client.getModeString() : "",
            Util.paint(client.username, client.color), channel,
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
