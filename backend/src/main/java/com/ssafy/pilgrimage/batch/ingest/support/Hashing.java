package com.ssafy.pilgrimage.batch.ingest.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {

	private Hashing() {
	}

	public static byte[] sha256(String value) {
		return sha256(value.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] sha256(byte[] bytes) {
		MessageDigest digest = digest();
		return digest.digest(bytes);
	}

	public static byte[] sha256(Path file) throws IOException {
		MessageDigest digest = digest();
		byte[] buffer = new byte[8192];
		try (InputStream input = Files.newInputStream(file)) {
			int read;
			while ((read = input.read(buffer)) != -1) {
				digest.update(buffer, 0, read);
			}
		}
		return digest.digest();
	}

	public static String hex(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		return HexFormat.of().formatHex(bytes);
	}

	public static byte[] fromHex(String hex) {
		return HexFormat.of().parseHex(hex);
	}

	private static MessageDigest digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}
