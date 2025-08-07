package com.javen.common.whoami.dto;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/9 06:59
 */
public enum SingletonEnum
{
    INSTANCE,
    INSTANCE2;;

    private Car car = new Car();

    public Car getCar()
    {
        return car;
    }

    public void setCar(Car car)
    {
        this.car = car;
    }
}
