package com.moonback.pilgrimage.batch.ingest.support;

public class NonRetryableRemoteException extends RuntimeException {

	public NonRetryableRemoteException(String message) {
		super(message);
	}

	public NonRetryableRemoteException(String message, Throwable cause) {
		super(message, cause);
	}
}
