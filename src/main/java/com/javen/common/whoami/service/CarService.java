package com.javen.common.whoami.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author: javen
 * @CreateTime: 2025/6/22 09:04
 */
@Service
@Slf4j
public class CarService
{
    public Integer add(Integer a, Integer b) {
      log.info("a + b = {}", a + b);
        return a + b;
    }

    public Integer minus(Integer a, Integer b) {
        log.info("a - b = {}", a - b);
        return a - b;
    }
}
