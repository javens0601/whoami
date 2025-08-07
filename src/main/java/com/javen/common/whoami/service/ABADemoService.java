package com.javen.common.whoami.service;

import com.javen.common.whoami.dto.Car;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/10 06:57
 */
public class ABADemoService
{
    public static void main(String[] args)
    {
        Car biyadi = new Car("biyadi");
        AtomicStampedReference<Car> atomicStampedReference = new AtomicStampedReference<>(biyadi, 1);
        System.out.println(atomicStampedReference.getReference() + " " + atomicStampedReference.getStamp());

        Car xiaomi = new Car("xiaomi");
        atomicStampedReference.compareAndSet(biyadi, xiaomi, atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
        System.out.println(atomicStampedReference.getReference() + " " + atomicStampedReference.getStamp());

        atomicStampedReference.compareAndSet(xiaomi, biyadi, atomicStampedReference.getStamp(), atomicStampedReference.getStamp() + 1);
        System.out.println(atomicStampedReference.getReference() + " " + atomicStampedReference.getStamp());
    }
}
