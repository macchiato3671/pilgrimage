package com.ssafy.pilgrimage.batch.ingest.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class TmdbClientTest {

	private MockWebServer server;
	private TmdbClient client;

	@BeforeEach
	void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		PilgrimageProperties properties = new PilgrimageProperties();
		properties.getTmdb().setBaseUrl(server.url("").toString());
		properties.getTmdb().setReadToken("test-token");
		properties.getTmdb().setRequestPerSecond(100);
		client = new TmdbClient(properties, new ObjectMapper());
	}

	@AfterEach
	void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	void searchTvSuccessUsesBearerToken() throws Exception {
		server.enqueue(json("""
				{"results":[{"id":1,"name":"참교육","original_name":"참교육","original_language":"ko","origin_country":["KR"],"first_air_date":"2025-01-01","popularity":10.0}]}
				"""));

		var response = client.searchTv("참교육");

		assertThat(response.results()).hasSize(1);
		assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer test-token");
	}

	@Test
	void emptySearchResultIsReturnedAsEmptyList() {
		server.enqueue(json("{\"results\":[]}"));

		assertThat(client.searchTv("없는 드라마").results()).isEmpty();
	}

	@Test
	void retriesAfter429() throws Exception {
		server.enqueue(new MockResponse().setResponseCode(429).setHeader("Retry-After", "0"));
		server.enqueue(json("{\"results\":[]}"));

		client.searchTv("참교육");

		assertThat(server.getRequestCount()).isEqualTo(2);
	}

	private MockResponse json(String body) {
		return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body);
	}
}
