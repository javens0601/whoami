package com.javen.common.whoami.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/9 21:35
 */
public class CASDemoService
{
    AtomicReference<Thread> atomicReference = new AtomicReference<>();

    private void lock() {
        Thread thread = Thread.currentThread();
        System.out.println(thread.getName() + " come in");
        while (!atomicReference.compareAndSet(null, thread)) {
            //System.out.println("-- " + thread.getName());
            try
            {
                TimeUnit.MILLISECONDS.sleep(100);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private void unLock() {
        Thread thread = Thread.currentThread();
        boolean b = atomicReference.compareAndSet(thread, null);
        System.out.println(thread.getName() + " unlock res " + b);
        if (b) {
            System.out.println(thread.getName() + " unlock success");
        }
    }

    public static void main(String[] args)
    {
        CASDemoService casDemoService = new CASDemoService();
        new Thread(() -> {
            casDemoService.lock();
            System.out.println(Thread.currentThread().getName() + " get lock success");
            try
            {
                TimeUnit.SECONDS.sleep(5);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            casDemoService.unLock();

        }, "a").start();
        try
        {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            casDemoService.lock();
            System.out.println(Thread.currentThread().getName() + " get lock success");

            casDemoService.unLock();

        }, "b").start();
    }


}
