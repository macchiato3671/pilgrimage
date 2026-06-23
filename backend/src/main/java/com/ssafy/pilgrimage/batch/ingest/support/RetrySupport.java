package com.ssafy.pilgrimage.batch.ingest.support;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public final class RetrySupport {

	private static final Duration[] DEFAULT_DELAYS = {
			Duration.ofSeconds(2),
			Duration.ofSeconds(5),
			Duration.ofSeconds(15)
	};

	private RetrySupport() {
	}

	public static <T> T withDefaultRetry(Supplier<T> supplier) {
		RetryableRemoteException last = null;
		for (int attempt = 1; attempt <= 3; attempt++) {
			try {
				return supplier.get();
			} catch (RetryableRemoteException e) {
				last = e;
				if (attempt == 3) {
					break;
				}
				Duration delay = e.retryAfter() != null ? e.retryAfter() : DEFAULT_DELAYS[attempt - 1];
				SimpleRateLimiter.sleep(withJitter(delay));
			}
		}
		throw last;
	}

	private static Duration withJitter(Duration base) {
		long millis = base.toMillis();
		long jitter = Math.max(1, millis / 5);
		return Duration.ofMillis(Math.max(0, millis + ThreadLocalRandom.current().nextLong(-jitter, jitter + 1)));
	}
}
