package com.example.pushnode.aip.msg;

import lombok.Data;

@Data
public class MessageModel {
    private String id;
    private byte[] content;
    private String dstDeviceId;
    private String srcDeviceId;
    private boolean offline = false;
}
