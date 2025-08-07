package com.javen.common.whoami.service;

import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/11 07:09
 */
public class AtomicIntegerFieldUpdaterService
{
    public static void main(String[] args) throws Exception
    {
        Account account = new Account();
        CountDownLatch countDownLatch = new CountDownLatch(200);
        StopWatch sw = new StopWatch();
        sw.start();
        for (int i = 0; i < 200; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10000; j++)
                {
                    //account.syncTransfer();
                    account.transfer();
                }
                countDownLatch.countDown();
            }).start();
        }
        countDownLatch.await();
        sw.stop();
        System.out.println(sw.getTotalTimeMillis());

        System.out.println(account.money);
    }

    static class Account {
        private String name;
        private volatile int money;

        AtomicIntegerFieldUpdater<Account> fieldUpdater = AtomicIntegerFieldUpdater.newUpdater(Account.class, "money");

        public void transfer() {
            fieldUpdater.getAndAdd(this, 1);
        }

        public synchronized void syncTransfer() {
            money++;
        }
    }
}

class MyAccount {

}
