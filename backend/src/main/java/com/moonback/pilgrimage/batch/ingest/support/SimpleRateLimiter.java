package com.moonback.pilgrimage.batch.ingest.support;

import java.time.Duration;

public class SimpleRateLimiter {

	private final long intervalMillis;
	private long nextAllowedAt;

	public SimpleRateLimiter(int requestPerSecond) {
		int safeRate = Math.max(1, requestPerSecond);
		this.intervalMillis = Math.max(1, 1000L / safeRate);
	}

	public synchronized void acquire() {
		long now = System.currentTimeMillis();
		if (nextAllowedAt > now) {
			sleep(Duration.ofMillis(nextAllowedAt - now));
			now = System.currentTimeMillis();
		}
		nextAllowedAt = now + intervalMillis;
	}

	public static void sleep(Duration duration) {
		try {
			Thread.sleep(Math.max(0, duration.toMillis()));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while rate limiting", e);
		}
	}
}
