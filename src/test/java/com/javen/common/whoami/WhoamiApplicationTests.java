package com.javen.common.whoami;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class WhoamiApplicationTests
{

    static void p()
    {
        System.out.println("B");
    }

    public static void main(String[] args) throws InterruptedException
    {
        Thread t1 = new Thread()
        {
            public void start()
            {
                System.out.println("C");
            }

            public void run()
            {
                p();
            }
        };
        t1.start();
        System.out.println("A");

        /*Thread t1 = new MtTask();
        t1.start();
        System.out.println("A");*/

    }

    static class  MtTask extends Thread {
        @Override
        public void run()
        {
            System.out.println("run");
        }

        @Override
        public synchronized void start()
        {
            System.out.println("start");
        }
    }

}
