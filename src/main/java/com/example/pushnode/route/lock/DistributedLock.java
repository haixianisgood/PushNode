package com.example.pushnode.route.lock;

public interface DistributedLock {
    void lock();
    void unlock();
}
