package com.javen.common.whoami.service;

import org.springframework.util.StopWatch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongBinaryOperator;


class AddNumber {
    long number;

    public synchronized void addBySynchronized() {
        number++;
    }

    AtomicLong atomicLong = new AtomicLong(0);
    public void addByAtomicLong() {
        atomicLong.getAndIncrement();
    }

    LongAdder longAdder = new LongAdder();
    public void addByLongAdder() {
        longAdder.increment();
    }

    LongAccumulator longAccumulator = new LongAccumulator(new LongBinaryOperator()
    {
        @Override
        public long applyAsLong(long left, long right)
        {
            return left + right;
        }
    }, 0);
    public void addByLongAccumulator() {
        longAccumulator.accumulate(1);
    }
}


/**
 * 50 个线程，每个线程累加500W次，算结果，对比4种实现的耗时
 */
public class LongAdderService
{

    static final CountDownLatch countDownLatch1 = new CountDownLatch(50);
    static final CountDownLatch countDownLatch2 = new CountDownLatch(50);
    static final CountDownLatch countDownLatch3 = new CountDownLatch(50);
    static final CountDownLatch countDownLatch4 = new CountDownLatch(50);
    static final Integer _1W = 10000;

    public static void main(String[] args) throws Exception
    {

        AddNumber addNumber = new AddNumber();

        // addBySynchronized
        StopWatch stopWatch1 = new StopWatch();
        stopWatch1.start("addBySynchronized");
        for (int i = 0; i < 50; i++)
        {
            new Thread(() -> {
                for (int j = 0; j < 500 * _1W; j++)
                {
                    addNumber.addBySynchronized();
                }
                countDownLatch1.countDown();
            }).start();
        }
        countDownLatch1.await();
        stopWatch1.stop();
        System.out.println("res: " + addNumber.number + "\taddBySynchronized\t耗时：" + stopWatch1.getTotalTimeMillis());

        // addByAtomicLong
        StopWatch stopWatch2 = new StopWatch();
        stopWatch2.start("addByAtomicLong");
        for (int i = 0; i < 50; i++)
        {
            new Thread(() -> {
            for (int j = 0; j < 500 * _1W; j++)
            {
                addNumber.addByAtomicLong();
            }
            countDownLatch2.countDown();
            }).start();
        }
        countDownLatch2.await();
        stopWatch2.stop();
        System.out.println("res: " + addNumber.atomicLong.get() + "\taddByAtomicLong\t耗时：" + stopWatch2.getTotalTimeMillis());

        // addByLongAdder
        StopWatch stopWatch3 = new StopWatch();
        stopWatch3.start("addByLongAdder");
        for (int i = 0; i < 50; i++)
        {
            new Thread(() -> {
            for (int j = 0; j < 500 * _1W; j++)
            {
                addNumber.addByLongAdder();
            }
            countDownLatch3.countDown();
            }).start();
        }
        countDownLatch3.await();
        stopWatch3.stop();
        System.out.println("res: " + addNumber.longAdder.longValue() + "\taddByLongAdder\t耗时：" + stopWatch3.getTotalTimeMillis());

        // addByLongAccumulator
        StopWatch stopWatch4 = new StopWatch();
        stopWatch4.start("addByLongAccumulator");
        for (int i = 0; i < 50; i++)
        {
            new Thread(() -> {
            for (int j = 0; j < 500 * _1W; j++)
            {
                addNumber.addByLongAccumulator();
            }
            countDownLatch4.countDown();
            }).start();
        }
        countDownLatch4.await();
        stopWatch4.stop();
        System.out.println("res: " + addNumber.longAccumulator.longValue() + "\taddByLongAccumulator\t耗时：" + stopWatch4.getTotalTimeMillis());

    }
}
