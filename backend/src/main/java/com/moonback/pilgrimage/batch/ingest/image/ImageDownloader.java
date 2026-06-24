package com.moonback.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.stereotype.Component;

import com.moonback.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.moonback.pilgrimage.batch.ingest.support.NonRetryableRemoteException;
import com.moonback.pilgrimage.batch.ingest.support.RetrySupport;
import com.moonback.pilgrimage.batch.ingest.support.RetryableRemoteException;

@Component
public class ImageDownloader {

	private final PilgrimageProperties properties;
	private final HttpClient httpClient;

	public ImageDownloader(PilgrimageProperties properties) {
		this.properties = properties;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	public DownloadedImage download(String sourceUrl) {
		return RetrySupport.withDefaultRetry(() -> download(sourceUrl, 0));
	}

	private DownloadedImage download(String sourceUrl, int redirects) {
		if (redirects > 5) {
			throw new NonRetryableRemoteException("Image download exceeded maximum redirects");
		}
		Path target = null;
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
					.timeout(Duration.ofSeconds(30))
					.header("User-Agent", properties.getCrawler().getUserAgent())
					.GET()
					.build();
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			int status = response.statusCode();
			if (status >= 300 && status < 400) {
				String location = response.headers().firstValue("Location")
						.orElseThrow(() -> new NonRetryableRemoteException("Redirect without Location header"));
				return download(URI.create(sourceUrl).resolve(location).toString(), redirects + 1);
			}
			if (status == 429 || (status >= 500 && status <= 504)) {
				throw new RetryableRemoteException("Image server returned HTTP " + status);
			}
			if (status != 200) {
				throw new NonRetryableRemoteException("Image server returned HTTP " + status);
			}
			String contentType = response.headers().firstValue("Content-Type").orElse("");
			if (!contentType.toLowerCase().startsWith("image/")) {
				throw new NonRetryableRemoteException("Response is not an image: " + contentType);
			}
			long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
			if (contentLength > properties.getImage().getMaxDownloadBytes()) {
				throw new NonRetryableRemoteException("Image exceeds max download size");
			}
			Files.createDirectories(properties.getImage().getTempDir());
			target = Files.createTempFile(properties.getImage().getTempDir(), "pilgrimage-original-", ".img");
			long bytes = copyWithLimit(response.body(), target, properties.getImage().getMaxDownloadBytes());
			return new DownloadedImage(target, contentType, bytes);
		} catch (IOException e) {
			deleteQuietly(target);
			throw new RetryableRemoteException("Image download failed", e);
		} catch (RuntimeException e) {
			deleteQuietly(target);
			throw e;
		} catch (InterruptedException e) {
			deleteQuietly(target);
			Thread.currentThread().interrupt();
			throw new RetryableRemoteException("Image download interrupted", e);
		}
	}

	private long copyWithLimit(InputStream input, Path target, long maxBytes) throws IOException {
		long total = 0;
		byte[] buffer = new byte[8192];
		try (InputStream in = input; OutputStream out = Files.newOutputStream(target)) {
			int read;
			while ((read = in.read(buffer)) != -1) {
				total += read;
				if (total > maxBytes) {
					throw new NonRetryableRemoteException("Image exceeds max download size");
				}
				out.write(buffer, 0, read);
			}
		}
		return total;
	}

	private void deleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// Best-effort cleanup only.
		}
	}
}
