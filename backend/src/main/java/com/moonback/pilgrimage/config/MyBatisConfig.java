package com.moonback.pilgrimage.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.moonback.pilgrimage.model.mapper")
public class MyBatisConfig {

}
