package com.ssafy.pilgrimage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/* 
 * Swagger api를 위한 config, controller를 자동으로 읽어서 api 문서를 만든다.
 * springboot 실행 후 http://localhost:8080/swagger-ui.html 주소창에 입력
 */
@Configuration
public class SwaggerConfig {
	
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Pilgrimage API")
						.description("드라마 촬영지 기반 여행 일정 서비스 API")
						.version("v1"));
	}
}
