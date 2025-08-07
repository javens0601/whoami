package com.javen.common.whoami.service;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/7 07:45
 */
public class ThreadSafeService
{

    private volatile int value = 0;

    // 利用volatile保证读取操作的可见性
    public int getValue()
    {
        return value;
    }

    // 利用synchronized保证复合操作的原子性
    public synchronized int setValue(int value)
    {
        return ++value;
    }
}
