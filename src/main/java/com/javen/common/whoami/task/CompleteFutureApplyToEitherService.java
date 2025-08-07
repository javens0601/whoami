package com.javen.common.whoami.task;

import com.javen.common.whoami.dto.Car;

import java.util.concurrent.CompletableFuture;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/6/30 08:24
 */
public class CompleteFutureApplyToEitherService
{
    public static void main(String[] args) throws Exception
    {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() ->
            {
            String s = Car.makeBigCar("liming", 5L);
            System.out.println("liming finish");
            return s;
            });

        CompletableFuture<String> completableFuture2 = CompletableFuture.supplyAsync(() ->
            {
            String s = Car.makeBigCar("xiaohong", 1L);
            System.out.println("xiaohong finish");
            return s;
            });

        CompletableFuture<String> completableFutureRes = completableFuture.applyToEither(completableFuture2, r ->
            {
            return r;
            });

        System.out.println(completableFutureRes.join());
    }
}
