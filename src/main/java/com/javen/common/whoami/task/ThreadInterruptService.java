package com.javen.common.whoami.task;

import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/4 15:55
 */
public class ThreadInterruptService
{
    public static void main(String[] args)
    {
        m8();
    }

    private static void m1() {
        Thread thread = new Thread(() ->
            {
            System.out.println("come in");
            try
            {
                TimeUnit.SECONDS.sleep(5);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            boolean interrupted = Thread.interrupted();
            System.out.println(Thread.currentThread().getName() + " " +   interrupted);
            System.out.println("a");
            });
        thread.start();
        boolean interrupted = Thread.interrupted();
        System.out.println(Thread.currentThread().getName() + " " +   interrupted);
        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        thread.interrupt();
    }

    private static volatile boolean isStop = false;
    private static void m2() {
        new Thread(() -> {
            while (true) {
                if (isStop) {
                    System.out.println("isStop true, break");
                    break;
                }
                System.out.println("-- hello --");
            }
        }, "a").start();

        new Thread(() -> {
        try
        {
            TimeUnit.MICROSECONDS.sleep(5);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        isStop = true;
        }, "b").start();
    }

    private static void m3() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        new Thread(() -> {
        while (true) {
            if (atomicBoolean.get()) {
                System.out.println("isStop true, break");
                break;
            }
            System.out.println("-- hello --");
        }
        }, "a").start();

        new Thread(() -> {
        try
        {
            TimeUnit.MICROSECONDS.sleep(1);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        atomicBoolean.set(true);
        }, "b").start();
    }

    private static void m4() {
        Thread t1 = new Thread(() ->
            {
            for (int i = 0; i < 3000; i++)
            {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("t1 isInterrupted");
                    break;
                }
                System.out.println("-- hello " + i);

                try
                {
                    TimeUnit.MILLISECONDS.sleep(200);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            }, "a");
        t1.start();


        try
        {
            TimeUnit.MILLISECONDS.sleep(4);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        t1.interrupt();
        System.out.println("t1.isInterrupted() : " + t1.isInterrupted());
    }

    static final Object objectLock = new Object();
    private static void m5() {
        Thread t1 = new Thread(() ->
            {
                synchronized (objectLock) {
                    System.out.println("-- a come in");
                    try
                    {
                        objectLock.wait();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                    System.out.println("-- a recover");
                }
            }, "a");
        t1.start();


        try
        {
            TimeUnit.MILLISECONDS.sleep(4);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            synchronized (objectLock)
            {
                System.out.println("-- notify");
                objectLock.notify();
            }
        }, "b").start();
    }

    private static void m6() {
        Thread t1 = new Thread(() ->
            {
            try
            {
                TimeUnit.SECONDS.sleep(2);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            synchronized (objectLock)
            {
                System.out.println("-- a come in");
                try
                {
                    objectLock.wait();
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                System.out.println("-- a recover");
            }
            }, "a");
        t1.start();


        try
        {
            TimeUnit.MILLISECONDS.sleep(4);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
        synchronized (objectLock)
        {
            System.out.println("-- notify");
            objectLock.notify();
        }
        }, "b").start();
    }

    private static void m7() {
        final Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        new Thread(() -> {
            lock.lock();
            try
            {
                System.out.println("-- a come in");
                condition.await();
                System.out.println("-- a recover");
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            finally
            {
                lock.unlock();
            }
        }, "a").start();

        new Thread(() -> {
            lock.lock();
            try
            {
                System.out.println("-- b signal");
                condition.signal();
            } finally {
                lock.unlock();
            }

        }, "b").start();
    }

    private static void m8() {
        Object lockObject = new Object();
        Thread t1 = new Thread(() ->
            {

            System.out.println("-- a come in");
            LockSupport.park(lockObject);
            System.out.println("-- a recover");
            System.out.println(LockSupport.getBlocker(Thread.currentThread()));
            }, "a");
        t1.start();

        new Thread(() -> {
        System.out.println("-- b unpark a");
        LockSupport.unpark(t1);
        }, "b").start();


    }
}
