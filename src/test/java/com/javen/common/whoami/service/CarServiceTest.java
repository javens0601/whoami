package com.javen.common.whoami.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @Description
 * @Author: enmonster
 * @CreateTime: 2025/6/22 09:08
 */
@SpringBootTest
class CarServiceTest
{

    @MockitoBean
    private CarService carService;
    @MockitoSpyBean
    private CarService spyCarService;

    @BeforeEach
    public void init() {
        when(carService.add(1,2))
                .thenReturn(3,6,7,8);
        when(carService.add(2,3))
                .thenThrow(IllegalArgumentException.class);
        when(carService.add(3,4)).thenCallRealMethod();
    }

    @Test
    void add()
    {
        Integer add = carService.add(1, 2);
        assertEquals(3, add);;
    }

    @Test
    void addThrow()
    {
        assertThrows(IllegalArgumentException.class, () -> {
            Integer add = carService.add(2, 3);
        });
    }

    @Test
    void addRealCall()
    {
        Integer add = carService.add(3, 4);
        assertEquals(7, add);
    }

    @Test
    void addSpyCall()
    {
        Integer add = spyCarService.add(3, 4);
        assertEquals(7, add);
    }

    @Test
    void addSpyCallSpy()
    {
        when(spyCarService.add(3,4))
                .thenReturn(8);
        Integer add = carService.add(3, 4);
        assertEquals(8, add);
    }

    @Test
    void minus()
    {
    }
}