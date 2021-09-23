package com.example.pushnode.aip.msg;

import lombok.Data;

@Data
public class MessageWrapper {
    private int type;
    private String token;

    private AuthModel authModel;
    private MessageModel messageModel;
    private AckModel ackModel;
    private SystemMessage systemMessage;


    public boolean isAuth() {
        return type == MessageType.TYPE_AUTH;
    }

    public boolean isMessage() {
        return type == MessageType.TYPE_MESSAGE;
    }

    public boolean isAck() {
        return type == MessageType.TYPE_ACK;
    }

    public boolean isSystemMessage() {
        return type == MessageType.TYPE_SYSTEM_MESSAGE;
    }

    public boolean isHeartbeat() {
        return type == MessageType.TYPE_HEARTBEAT;
    }

    public Object getPayload() {
        if(isAuth()) {
            return authModel;
        }

        if(isMessage()) {
            return messageModel;
        }

        if(isAck()) {
            return ackModel;
        }

        return null;
    }
}
