package com.javen.common.whoami.service;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/22 07:39
 */
public class JOLDemoService
{
    public static void main(String[] args) throws Exception
    {
        //TimeUnit.SECONDS.sleep(5);
        //System.out.println(VM.current().details());
        Object obj = new Object();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        new Thread(() -> {
            synchronized (obj)
            {
                System.out.println(ClassLayout.parseInstance(obj).toPrintable());
            }
        }).start();
    }
}

class JOL {
    String name;
    Long age;
    Double aDouble;
    BigDecimal bigDecimal;
    int myInt;
    char myChar;
    short aShort;
    boolean bool;
}
