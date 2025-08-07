package com.javen.common.whoami.service;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/21 16:34
 */
// 父子线程传递
// 仅仅是简单场景下的父子线程传递

public class InheritableThreadLocalService
{
    //private static ThreadLocal<Integer> inheritableThreadLocal = InheritableThreadLocal.<Integer>withInitial(() -> 0);
    private static InheritableThreadLocal<Integer> inheritableThreadLocal = new InheritableThreadLocal<Integer>();
    private static ExecutorService executors = Executors.newFixedThreadPool(3);
    public static void main(String[] args) throws Exception
    {
        inheritableThreadLocal.set(1);

        Integer i = inheritableThreadLocal.get();
        System.out.println("主线程:" + i);

        /*new Thread(() -> {
            Integer si = inheritableThreadLocal.get();
            System.out.println("子线程:" + si);
        }).start();*/

        executors.submit(() -> {
            Integer si = inheritableThreadLocal.get();
            System.out.println("子线程:" + si);
            inheritableThreadLocal.remove();
        });

        TimeUnit.MILLISECONDS.sleep(500);

        inheritableThreadLocal.set(2);
        Integer i2 = inheritableThreadLocal.get();
        System.out.println("主线程:" + i2);

        for (int k = 0; k < 10; k++) {
            executors.submit(() -> {
            Integer si2 = inheritableThreadLocal.get();
            System.out.println("子线程:" + si2);
            inheritableThreadLocal.remove();
            });
        }

        TimeUnit.MILLISECONDS.sleep(500);

        executors.shutdown();

    }
}
