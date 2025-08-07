package com.javen.common.whoami.task;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/3 21:59
 */
public class DeadLockService
{
    public static void main(String[] args)
    {
        Object objA = new Object();
        Object objB = new Object();

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.submit(() -> {
            synchronized (objA) {
                System.out.println("a come in");
                try
                {
                    TimeUnit.SECONDS.sleep(1);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                synchronized (objB) {
                    System.out.println("a get lock b");
                }
            }
        });

        executorService.submit(() -> {
        synchronized (objB) {
            System.out.println("b come in");
            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            synchronized (objA) {
                System.out.println("b get lock a");
            }
        }
        });
    }
}
