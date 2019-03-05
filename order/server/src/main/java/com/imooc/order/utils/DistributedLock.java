package com.imooc.order.utils;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;


public class DistributedLock {
    private static final int MAX_RETRY_COUNT = 5;
    //zookeeper实现分布式锁
    public void test() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient("localhost", retryPolicy);
        client.start();
        InterProcessMutex mutex = new InterProcessMutex(client, "/locks");
        int i = MAX_RETRY_COUNT;
        while (i > 0) {
            try {
                //acquire获取锁存在通信异常时，会抛出异常，设置重试次数为5
                if (mutex.acquire(3, TimeUnit.SECONDS)) {
                    // doSomething
                    mutex.release();
                    break;
                }
            } catch (Exception e) {
                i--;
                e.printStackTrace();
            } finally {
                client.close();
            }
        }

    }
}
