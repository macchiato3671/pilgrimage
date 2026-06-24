package com.ssafy.pilgrimage.batch.ingest.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.support.NonRetryableRemoteException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImageInspector {

	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
	private static final byte[] GIF87 = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
	private static final byte[] GIF89 = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
	private static final Pattern SVG_WIDTH = Pattern.compile("\\bwidth=['\"]?(\\d+)");
	private static final Pattern SVG_HEIGHT = Pattern.compile("\\bheight=['\"]?(\\d+)");

	private final PilgrimageProperties properties;

	public ImageInspection inspect(Path file) {
		try {
			byte[] header = readHeader(file);
			ImageFormat format = format(header);
			int width = 0;
			int height = 0;
			if (format == ImageFormat.SVG) {
				int[] dimensions = svgDimensions(file);
				width = dimensions[0];
				height = dimensions[1];
			} else if (format != ImageFormat.WEBP) {
				BufferedImage image = ImageIO.read(file.toFile());
				if (image == null) {
					throw new NonRetryableRemoteException("Image cannot be decoded");
				}
				width = image.getWidth();
				height = image.getHeight();
			}
			if (width > 0 && height > 0 && (long) width * height > properties.getImage().getMaxPixels()) {
				throw new NonRetryableRemoteException("Image exceeds max pixel count");
			}
			return new ImageInspection(format, width, height);
		} catch (IOException e) {
			throw new NonRetryableRemoteException("Image inspection failed", e);
		}
	}

	private byte[] readHeader(Path file) throws IOException {
		byte[] bytes = new byte[512];
		try (InputStream input = Files.newInputStream(file)) {
			int read = input.read(bytes);
			if (read <= 0) {
				return new byte[0];
			}
			return java.util.Arrays.copyOf(bytes, read);
		}
	}

	private ImageFormat format(byte[] header) {
		if (header.length >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8) {
			return ImageFormat.JPEG;
		}
		if (startsWith(header, PNG)) {
			return ImageFormat.PNG;
		}
		if (startsWith(header, GIF87) || startsWith(header, GIF89)) {
			return ImageFormat.GIF;
		}
		if (header.length >= 12 && new String(header, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
				&& new String(header, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
			return ImageFormat.WEBP;
		}
		String text = new String(header, StandardCharsets.UTF_8).trim().toLowerCase();
		if (text.startsWith("<svg") || text.contains("<svg")) {
			return ImageFormat.SVG;
		}
		throw new NonRetryableRemoteException("Unsupported or corrupted image");
	}

	private boolean startsWith(byte[] value, byte[] prefix) {
		if (value.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (value[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	private int[] svgDimensions(Path file) throws IOException {
		String text = Files.readString(file, StandardCharsets.UTF_8);
		return new int[] {extractDimension(SVG_WIDTH, text), extractDimension(SVG_HEIGHT, text)};
	}

	private int extractDimension(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
	}
}
