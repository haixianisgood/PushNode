package com.example.pushnode.route;

import com.example.pushnode.route.lock.DistributedLock;
import com.example.pushnode.route.lock.ZooKeeperLock;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 路由服务的实现。
 * 用于保存和管理推送节点、客户端设备的信息
 */
@Service("routeService")
public class RouteServiceImpl implements RouteService{
    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${appconfig.push.host}"+":"+"${appconfig.push.port}")
    private String address;

    @Value("${appconfig.push.name}")
    private String nodeName;

    @Override
    public void addPushNode(String name) {
        try {
            Stat stat = curatorFramework.checkExists().forPath("/Push/"+name);
            if(stat != null) {
                curatorFramework.delete()
                        .guaranteed()
                        .forPath("/Push/"+name);
            }

            //持久节点，作为设备的父节点
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/Push/"+name, address.getBytes(StandardCharsets.UTF_8));

            //临时节点，作为推送管理系统的子节点
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath("/PushManager/"+name, address.getBytes(StandardCharsets.UTF_8));

            //把节点名和地址添加到路由表
            RMap<String, String> pushNodeMap = redissonClient.getMap("PushNodeMap");
            pushNodeMap.put(name, address);

            //子节点的监听器
            CuratorCache cache = CuratorCache.build(curatorFramework, "/Push/"+name);
            CuratorCacheListener listener = CuratorCacheListener.builder()
                    .forPathChildrenCache("/Push/"+name, curatorFramework, new PathChildrenCacheListener() {
                        @Override
                        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                            //新建子节点，即设备上线
                            if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_ADDED)) {
                                ChildData childData = event.getData();
                                String deviceId = new String(childData.getData(), StandardCharsets.UTF_8);
                                onDeviceOnline(deviceId);

                                System.out.println("device add: "+childData.getPath());
                            }

                            //删除子节点，即设备下线
                            if(event.getType().equals(PathChildrenCacheEvent.Type.CHILD_REMOVED)) {
                                ChildData childData = event.getData();
                                String deviceId = new String(childData.getData(), StandardCharsets.UTF_8);
                                onDeviceOffline(deviceId);

                                System.out.println("device remove: "+childData.getPath());
                            }
                        }
                    })
                    .build();

            cache.listenable().addListener(listener);
            cache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deletePushNode(String name) {
        try {
            //更新推送节点路由表
            RMap<String, String> pushNodeMap = redissonClient.getMap("PushNodeMap");
            pushNodeMap.remove(name);

            //把推送节点相关信息从zookeeper中删除
            List<String> deviceIdList = curatorFramework.getChildren().forPath("/Push/"+name);
            for (String deviceId : deviceIdList) {
                deleteDevice(name, deviceId);
            }
            curatorFramework.delete().deletingChildrenIfNeeded().forPath("/Push/"+name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addDevice(String nodeName, String deviceId) {
        try {
            curatorFramework.create()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath("/Push/"+nodeName+"/"+deviceId, deviceId.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteDevice(String nodeName, String deviceId) {
        try {
            curatorFramework.delete().guaranteed().forPath("/Push/"+nodeName+"/"+deviceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isOnline(String deviceId) {
        RMap<String, String> deviceMap = redissonClient.getMap("device:"+deviceId);
        boolean isOnline = deviceMap.isExists();

        return isOnline;
    }

    @Override
    public String getPushNode(String deviceId) {
        String pushNode = null;

        RMap<String, String> deviceMap = redissonClient.getMap("device:"+deviceId);
        if(deviceMap.isExists()) {
            pushNode = deviceMap.get("PushNode");
        }

        return pushNode;
    }

    @Override
    public DistributedLock distributedLock(String lockName) {
        return new ZooKeeperLock(curatorFramework, lockName);
    }

    /**
     * 设备上线后进行的操作
     * @param deviceId 设备id
     */
    private void onDeviceOnline(String deviceId) {
        RMap<String, String> deviceMap = redissonClient.getMap("device:"+deviceId);
        deviceMap.put("PushNode", nodeName);
    }

    /**
     * 设备离线后进行的操作
     * @param deviceId 设备id
     */
    private void onDeviceOffline(String deviceId) {
        RMap<String, String> deviceMap = redissonClient.getMap("device:"+deviceId);
        deviceMap.delete();
    }
}
