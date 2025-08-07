package com.javen.common.whoami.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/6/24 08:01
 */
public class FutureTaskService {
    public static void main(String[] args) throws ExecutionException, InterruptedException
    {
        Thread t1 = new Thread(new MyTask(), "name-t1");
        t1.start();

        MyTask myTask = new MyTask();
        FutureTask<String> task = new FutureTask<String>(new MyTask());
        Thread t2 = new Thread(task, "name-t2");
        t2.start();
        String s = task.get();
        System.out.println(s);

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(fluent = true, chain = true)
    static class MyTask implements Callable<String>, Runnable {

        private String name;
        private String address;

        @Override
        public void run()
        {
            System.out.println(Thread.currentThread() + " hello runnable");
        }

        @Override
        public String call() throws Exception
        {

            System.out.println(Thread.currentThread() + " hello callable");
            return "hello callable";
        }
    }
    
}
