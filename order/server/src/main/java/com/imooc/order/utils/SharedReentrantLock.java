package com.imooc.order.utils;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * 测试可重入锁（可以多次获得锁不会被阻塞，释放时也需释放多把锁）
 * @author 76524
 *
 */
public class SharedReentrantLock implements Runnable{
    private InterProcessMutex lock;//可重入锁实现类
    private String lockPAth = "/lock/shareLock";
    private int i;
    private String clientName;
    //zookeeper集群地址
    public static final String ZOOKEEPERSTRING = "192.168.99.129:2181,192.168.99.153:2181,192.168.99.171:2181";

    public SharedReentrantLock(CuratorFramework client,String clientName) {
        lock = new InterProcessMutex(client, lockPAth);
        this.clientName = clientName;
    }

    @Override
    public void run() {
        try {
            Thread.sleep((new java.util.Random().nextInt(2000)));
            lock.acquire();  //增加第一把锁
            if(lock.isAcquiredInThisProcess()) {
                System.out.println(clientName + " 获得锁");
                i++;
            }
            lock.acquire();  //增加第二把锁
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {

                System.out.println(clientName+"释放第一把锁");
                lock.release();
                System.out.println(clientName+"释放第二把锁");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        CuratorFramework client = CuratorFrameworkFactory.newClient(ZOOKEEPERSTRING, new ExponentialBackoffRetry(1000, 3));
        client.start();
        //启动100个线程进行测试
        for(int i = 0;i<100;i++) {
            SharedReentrantLock sharedReentrantLock = new SharedReentrantLock(client, "第"+i+"个客户端：");
            Thread thread = new Thread(sharedReentrantLock);
            thread.start();
        }
    }

}
