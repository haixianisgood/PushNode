package com.example.pushnode.route.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

public class ZooKeeperLock implements DistributedLock{
    private InterProcessLock lock;

    public ZooKeeperLock(CuratorFramework curatorFramework, String lockName) {
        this.lock = new InterProcessMutex(curatorFramework, "/PushLock/"+lockName);
    }

    @Override
    public void lock() {
        try {
            lock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlock() {
        try {
            lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
