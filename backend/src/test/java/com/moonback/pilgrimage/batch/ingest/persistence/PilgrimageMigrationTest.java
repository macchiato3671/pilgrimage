package com.moonback.pilgrimage.batch.ingest.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moonback.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.moonback.pilgrimage.batch.ingest.model.DramaUpsert;
import com.moonback.pilgrimage.batch.ingest.model.GenreUpsert;
import com.moonback.pilgrimage.batch.ingest.model.IngestStatus;
import com.moonback.pilgrimage.batch.ingest.support.Hashing;

@Testcontainers(disabledWithoutDocker = true)
class PilgrimageMigrationTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("pilgrimage")
			.withUsername("test")
			.withPassword("test");

	@Test
	void migrationSucceedsAndDramaUpsertIsIdempotent() {
		Flyway.configure()
				.dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
				.locations("classpath:db/migration")
				.baselineOnMigrate(true)
				.baselineVersion("1")
				.load()
				.migrate();

		SqlSessionTemplate sqlSessionTemplate = sqlSessionTemplate(dataSource());
		PilgrimageIngestMapper mapper = sqlSessionTemplate.getMapper(PilgrimageIngestMapper.class);
		PilgrimageIngestRepository repository = new PilgrimageIngestRepository(mapper, new ObjectMapper());
		byte[] postKey = Hashing.sha256("https://ys-dl.tistory.com/100");
		repository.upsertDiscoveredPost(new com.moonback.pilgrimage.batch.ingest.model.DiscoveredPost(
				postKey, "https://ys-dl.tistory.com/100", "참교육 촬영지"));
		CrawlPostRow post = new CrawlPostRow(postKey, "https://ys-dl.tistory.com/100", "참교육 촬영지",
				null, "참교육", "참교육", null, null, IngestStatus.PARSED, null, null);
		DramaUpsert drama = new DramaUpsert(123, "참교육", "True Lesson", "ko", "KR",
				LocalDate.of(2025, 1, 1), "overview", List.of(new GenreUpsert(18, "Drama")));

		repository.saveTmdbMatched(post, drama, List.of());
		repository.saveTmdbMatched(post, drama, List.of());

		assertThat(mapper.countDrama()).isEqualTo(1);
		assertThat(mapper.countGenre()).isEqualTo(1);
		assertThat(mapper.countDramaGenre()).isEqualTo(1);
	}

	private SqlSessionTemplate sqlSessionTemplate(DriverManagerDataSource dataSource) {
		try {
			SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
			Configuration configuration = new Configuration();
			configuration.setMapUnderscoreToCamelCase(true);
			factoryBean.setConfiguration(configuration);
			factoryBean.setDataSource(dataSource);
			factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
			return new SqlSessionTemplate(factoryBean.getObject());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to create MyBatis test session", e);
		}
	}

	private DriverManagerDataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
		dataSource.setUrl(MYSQL.getJdbcUrl());
		dataSource.setUsername(MYSQL.getUsername());
		dataSource.setPassword(MYSQL.getPassword());
		return dataSource;
	}
}
