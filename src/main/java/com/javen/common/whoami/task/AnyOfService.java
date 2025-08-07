package com.javen.common.whoami.task;

import org.openjdk.jol.info.ClassLayout;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/22 15:30
 */
public class AnyOfService
{
    public static void main(String[] args) throws Exception
    {
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() ->
            {
            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            //throw new NullPointerException("1");
            //System.out.println(1);
            return 1;
            });

        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() ->
            {
            try
            {
                TimeUnit.SECONDS.sleep(5);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            //System.out.println(2);
            throw new NullPointerException("1");
            //return 2;
            });


        CompletableFuture.allOf(f1,f2).join();
    }
}
