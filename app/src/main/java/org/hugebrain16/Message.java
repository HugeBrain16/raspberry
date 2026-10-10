package org.hugebrain16;

import java.time.LocalDateTime;

public class Message {
    String sender;
    String content;
    LocalDateTime time;
    
    Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.time = LocalDateTime.now();
    }

    Message(Message message) {
        this.sender = message.sender;
        this.content = message.content;
        this.time = message.time;
    }
}
