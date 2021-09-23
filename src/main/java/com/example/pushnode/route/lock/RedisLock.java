package com.example.pushnode.route.lock;

import org.redisson.api.RLock;

public class RedisLock implements DistributedLock{
    private RLock lock;

    public RedisLock(RLock lock) {
        this.lock = lock;
    }

    @Override
    public void lock() {
        lock.lock();
    }

    @Override
    public void unlock() {
        lock.unlock();
    }
}
