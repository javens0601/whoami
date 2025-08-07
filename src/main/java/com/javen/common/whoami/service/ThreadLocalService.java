package com.javen.common.whoami.service;

import lombok.EqualsAndHashCode;

import java.math.MathContext;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

@EqualsAndHashCode
class  Car {
    LongAdder num = new LongAdder();

    /*ThreadLocal<Integer> subNum = new ThreadLocal<>() {
        @Override
        protected Integer initialValue()
        {
            return 0;
        }
    };*/

    public void acc(long x) {
        num.add(x);
    }

    //推荐使用这种方式进行初始化ThreadLocal
    ThreadLocal<Integer> subNum = ThreadLocal.withInitial(() -> 0);

    ThreadLocal<Integer> subNum2 = ThreadLocal.withInitial(() -> 0);

    public void addByThreadLocal() {
        subNum.set(1 + subNum.get());
    }
    public void addByThreadLocal2() {
        subNum2.set(subNum2.get() - 1);
    }
}

public class ThreadLocalService
{

    public static void main(String[] args) throws Exception
    {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Car car = new Car();
        for (int i = 0; i < 4; i++) {
            executorService.execute(
                    () ->
                        {
                        long n = ThreadLocalRandom.current().nextLong(1, 3);
                        try
                        {
                            for (int j = 0; j < n; j++)
                            {
                                car.acc(1);
                                car.addByThreadLocal();
                                car.addByThreadLocal2();
                            }
                            System.out.println("subNum \t" + car.subNum.get());
                            System.out.println("subNuM \t" + car.subNum2.get());
                        }
                        finally
                        {
                            //使用完成，需要释放ThreadLocal,必须的
                            //尤其在线程池的场景下，因为线程复用，不释放会导致业务逻辑出现错误
                            car.subNum.remove();
                            car.subNum2.remove();
                        }
                        }
            );
        }
        //调用 shutdown() 方法后，线程池会停止接受新的任务，但是会继续执行已经提交的任务。此方法不会立即终止线程池中的线程，而是等待它们自然结束。
        executorService.shutdown();
        TimeUnit.SECONDS.sleep(1);
        System.out.println("total: \t" + car.num.longValue());
    }

}
