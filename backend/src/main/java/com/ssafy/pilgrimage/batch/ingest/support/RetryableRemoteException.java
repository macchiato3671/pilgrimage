package com.ssafy.pilgrimage.batch.ingest.support;

import java.time.Duration;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class RetryableRemoteException extends RuntimeException {

	private final Duration retryAfter;

	public RetryableRemoteException(String message) {
		this(message, null, null);
	}

	public RetryableRemoteException(String message, Throwable cause) {
		this(message, cause, null);
	}

	public RetryableRemoteException(String message, Duration retryAfter) {
		this(message, null, retryAfter);
	}

	public RetryableRemoteException(String message, Throwable cause, Duration retryAfter) {
		super(message, cause);
		this.retryAfter = retryAfter;
	}

}
