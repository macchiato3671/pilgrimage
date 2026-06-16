package com.ssafy.pilgrimage.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.ssafy.pilgrimage.model.mapper")
public class MyBatisConfig {

}
