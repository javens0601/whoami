package com.javen.common.whoami.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/8/1 17:07
 *
 * 读锁启动的时候，写锁可介入
 */
public class StampedLockService
{
    public static void main(String[] args)
    {

    }

    // 读锁开启，写锁无法介入
    private void test1() {
        Car0807 car0807 = new Car0807();
        new Thread(() -> car0807.read()).start();
        new Thread(() -> car0807.write()).start();
    }
}

class Car0807 {
    public int num;

    StampedLock stampedLock = new StampedLock();

    // 悲观读
    public void read() {
        long l = stampedLock.readLock();
        try {
            System.out.println("read");
            TimeUnit.SECONDS.sleep(4);
        }
        catch (Exception e) {

        }
        finally {
            stampedLock.unlockRead(l);
        }
    }

    // 悲观写
    public void write() {
        System.out.println("write start");
        long l = stampedLock.writeLock();
        try {
            System.out.println("write");
        } finally {
            stampedLock.unlockWrite(l);
        }
    }

    //乐观读
    public void optimisticRead() {

    }
}
