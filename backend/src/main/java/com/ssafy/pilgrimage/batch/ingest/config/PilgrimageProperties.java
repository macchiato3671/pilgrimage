package com.ssafy.pilgrimage.batch.ingest.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@ConfigurationProperties(prefix = "")
public class PilgrimageProperties {

	private final Crawler crawler = new Crawler();
	private final Tmdb tmdb = new Tmdb();
	private final Kakao kakao = new Kakao();
	private final Storage storage = new Storage();
	private final Image image = new Image();

	@Getter
	@Setter
	public static class Crawler {
		private String categoryUrl = "https://ys-dl.tistory.com/category/촬영지/드라마%20촬영지";
		private long requestDelayMs = 1000;
		private Duration connectTimeout = Duration.ofSeconds(10);
		private Duration readTimeout = Duration.ofSeconds(30);
		private String userAgent = "PilgrimagePersonalBatch/1.0";
		private int maxPages = 0;
	}

	@Getter
	@Setter
	public static class Tmdb {
		private String baseUrl = "https://api.themoviedb.org/3";
		private String imageBaseUrl = "https://image.tmdb.org/t/p/original";
		private String readToken = "";
		private String language = "ko-KR";
		private int requestPerSecond = 5;
		private String imageLanguages = "";
	}

	@Getter
	@Setter
	public static class Kakao {
		private String baseUrl = "https://dapi.kakao.com";
		private String restApiKey = "";
		private int requestPerSecond = 5;
	}

	@Getter
	@Setter
	public static class Storage {
		private String region = "";
		private String bucket = "";
		private String publicBaseUrl = "";
		private String prefix = "pilgrimage";
	}

	@Getter
	@Setter
	public static class Image {
		private String cwebpPath = "cwebp";
		private String gif2webpPath = "gif2webp";
		private String imagemagickPath = "magick";
		private Path tempDir = Path.of("/tmp/pilgrimage-ingest");
		private long maxDownloadBytes = 26_214_400;
		private long maxPixels = 50_000_000;
		private int sceneMaxLongEdge = 1920;
		private int posterMaxLongEdge = 1200;
		private int backdropMaxLongEdge = 1920;
		private int staticQuality = 82;
		private int posterQuality = 85;
	}
}
