package com.example.pushnode.route;

import com.example.pushnode.route.lock.DistributedLock;

public interface RouteService {
    /**
     * 在推送系统中添加一个推送服务器节点
     * @param name 节点名
     */
    void addPushNode(String name);

    /**
     * 删除推送服务器节点
     * @param name 节点名
     */
    void deletePushNode(String name);

    /**
     * 往推送节点中添加一个设备
     * @param nodeName 节点名
     * @param deviceId 设备id
     */
    void addDevice(String nodeName, String deviceId);

    /**
     * 删除推送节点中的一个设备
     * @param nodeName 节点名
     * @param deviceId 设备id
     */
    void deleteDevice(String nodeName, String deviceId);

    /**
     * 查看设备是否在线
     * @param deviceId 设备id
     * @return true在线，false离线
     */
    boolean isOnline(String deviceId);

    /**
     * 获取为设备提供推送服务的节点
     * @param deviceId 设备id
     * @return 节点名
     */
    String getPushNode(String deviceId);

    /**
     * 获取一个分布式锁
     * @param lockName 需要加锁的资源
     * @return 分布式锁
     */
    DistributedLock distributedLock(String lockName);
}
