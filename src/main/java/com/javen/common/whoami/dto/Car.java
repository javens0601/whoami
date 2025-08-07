package com.javen.common.whoami.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/7/2 18:15
 */
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Car
{
    private String name;

    public synchronized String makeCar(String username) {
        return username + " make car";
    }

    public static synchronized String makeBigCar(String username, Long second) {
        try
        {
            TimeUnit.SECONDS.sleep(second);
        }
        catch (InterruptedException e)
        {
            //throw new RuntimeException(e);
        }
        return username + " make big car";
    }

    @Override
    public String toString()
    {
        return "Car{" +
                "name='" + name + '\'' +
                '}';
    }
}
