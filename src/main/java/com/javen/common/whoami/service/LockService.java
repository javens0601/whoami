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
 */p
public class LockService
{

    public static void main(String[] args)
    {
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
}