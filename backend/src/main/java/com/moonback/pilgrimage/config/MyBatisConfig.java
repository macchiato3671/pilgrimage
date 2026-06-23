package com.moonback.pilgrimage.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
		"com.moonback.pilgrimage.model.mapper",
		"com.moonback.pilgrimage.batch.ingest.persistence"
})
public class MyBatisConfig {

}
