package com.javen.common.whoami.task;

import com.javen.common.whoami.dto.Car;
import com.javen.common.whoami.dto.SingletonEnum;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/7/9 07:01
 */
public class SingletonService
{
    public static void main(String[] args)
    {
        Car car1 = SingletonEnum.INSTANCE.getCar();

        Car car2 = SingletonEnum.INSTANCE.getCar();

        System.out.println(car1 == car2);
    }
}
