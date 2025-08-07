package com.javen.common.whoami.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/18 11:08
 */
public class ExecutorSerService
{
    public static void main(String[] args) throws Exception
    {
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        //异常信息会包装在future中返回，不会直接抛错的
        Future<?> submit = executorService.submit(() ->
            {
            int i = 0;
            double s = 1 / i;
            });
        Object o = submit.get();
        System.out.println(o);

        //有异常会抛出，默认的异常处理策略，不影响后面的执行
        executorService.execute(() -> {
        int i = 0;
        double s = 1/i;
        });

        TimeUnit.SECONDS.sleep(1);
        System.out.println("end");
    }
}
