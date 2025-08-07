package com.javen.common.whoami.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/30 22:24
 */
public class LockService
{

    public static void main(String[] args)
    {
        //test1();
        //test2();
        test3();

    }

    // 读写互斥，读读并发
    private static void test1() {
        AccountResource resource = new AccountResource();

        for (int i = 0; i < 10; i++)
        {
            int finalI = i;
            new Thread(()-> {

                resource.write(finalI +"", finalI +"");
            }, "Thread write" + i)
                    .start();

        }

        for (int i = 0; i < 10; i++)
        {
            int finalI = i;
            new Thread(()-> {
                resource.read(finalI + "");
            }, "Thread read" + i)
                    .start();
        }

        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < 3; i++)
        {
            int finalI = i;
            new Thread(()-> {
                resource.write(finalI +"", finalI +"");
            }, "New Thread write" + i)
                    .start();

        }
    }

    // 锁降级 写后读
    private static void test2() {
        AccountResource resource = new AccountResource();
        for (int i = 0; i < 10; i++)
        {
            int finalI = i;
            new Thread(()-> {

                resource.lockDown(finalI +"", finalI +"");
            }, "Thread write" + i)
                    .start();

        }
    }

    // 锁降级卡死 读后写
    private static void test3() {
        AccountResource resource = new AccountResource();
        for (int i = 0; i < 10; i++)
        {
            int finalI = i;
            new Thread(()-> {

                resource.lockDownError(finalI +"", finalI +"");
            }, "Thread write" + i)
                    .start();

        }
    }
}

class AccountResource {

    Map<String, String> map = new ConcurrentHashMap<>();
    Lock lock = new ReentrantLock();
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void write(String key, String value) {
        try
        {
            //lock.lock();
            readWriteLock.writeLock().lock();
            try
            {
                TimeUnit.MILLISECONDS.sleep(300);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            System.out.println(Thread.currentThread().getName() + " write start");
            map.put(key, value);
            System.out.println(Thread.currentThread().getName() + " write end");
        } finally {
            //lock.unlock();
            readWriteLock.writeLock().unlock();
        }

    }

    public void read(String key) {
        try
        {
            //lock.lock();
            readWriteLock.readLock().lock();
            try
            {
                TimeUnit.SECONDS.sleep(4);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            System.out.println(Thread.currentThread().getName() + " read start");
            String val = map.get(key);
            System.out.println(Thread.currentThread().getName() + " read end" + val);
        } finally
        {
            //lock.unlock();
            readWriteLock.readLock().unlock();
        }

    }

    /**
     * 锁降级 写后读
     *
     * 写锁开启  --> 读锁开启 --> 写锁关闭 --降级成读锁--> 读锁关闭
     * 读锁开启的时候写锁无法介入
     * @param key
     * @param value
     */
    public void lockDown(String key, String value) {
        try {
            readWriteLock.writeLock().lock();
            System.out.println("写锁开启");
            map.put(key, value);

            readWriteLock.readLock().lock();
            System.out.println("读锁开启");
        } finally {
            readWriteLock.writeLock().unlock();
            System.out.println("写锁关闭");
        }

        try {
            String s = map.get(key);
            System.out.println(s);
        } finally {
            readWriteLock.readLock().unlock();
            System.out.println("读锁关闭\n");
        }
    }

    /**
     * 读后写，写锁无法介入，卡死
     * @param key
     * @param value
     */
    public void lockDownError(String key, String value) {
        try {
            readWriteLock.readLock().lock();
            System.out.println("读锁开启");
            map.put(key, value);

            readWriteLock.writeLock().lock();
            System.out.println("写锁开启");
        } finally {
            readWriteLock.readLock().unlock();
            System.out.println("读锁关闭");
        }

        try {
            String s = map.get(key);
            System.out.println(s);
        } finally {
            readWriteLock.writeLock().unlock();
            System.out.println("写锁关闭\n");
        }
    }
}