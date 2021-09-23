package com.example.pushnode.push;

import com.example.pushnode.aip.msg.MessageModel;

public interface PushServer {
    /**
     * 推送消息到设备
     * @param deviceId 设备id
     * @param message 消息
     */
    void pushToDevice(String deviceId, MessageModel message);

    /**
     * 开始运行
     */
    void start();

    /**
     * 关闭
     */
    void close();

    /**
     * 断开一个与客户端设备的连接
     * @param deviceId 设备id
     */
    void closeDevice(String deviceId);
}
