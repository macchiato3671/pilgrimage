package com.ssafy.pilgrimage.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
		"com.ssafy.pilgrimage.model.mapper",
		"com.ssafy.pilgrimage.batch.ingest.persistence"
})
public class MyBatisConfig {

}
