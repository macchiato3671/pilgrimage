package com.ssafy.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.model.ImageType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalProcessWebpConverter implements WebpConverter {

	private final PilgrimageProperties properties;
	private final ProcessRunner processRunner;

	@Override
	public ConvertedImage convert(Path input, ImageInspection inspection, ImageType imageType) {
		try {
			Files.createDirectories(properties.getImage().getTempDir());
			Path conversionInput = input;
			Path rasterized = null;
			if (inspection.format() == ImageFormat.SVG) {
				rasterized = Files.createTempFile(properties.getImage().getTempDir(), "pilgrimage-svg-", ".png");
				run(List.of(properties.getImage().getImagemagickPath(), input.toString(), "-background", "none",
						rasterized.toString()), "IMAGE_TOOL_MISSING");
				conversionInput = rasterized;
			}
			Path output = Files.createTempFile(properties.getImage().getTempDir(), "pilgrimage-webp-", ".webp");
			if (inspection.format() == ImageFormat.GIF) {
				run(gifCommand(conversionInput, output, imageType), "IMAGE_TOOL_MISSING");
			} else {
				run(cwebpCommand(conversionInput, output, inspection, imageType), "IMAGE_TOOL_MISSING");
			}
			if (!Files.exists(output) || Files.size(output) == 0 || !looksLikeWebp(output)) {
				throw new ImageProcessingException("IMAGE_CONVERSION_FAILED", "Converted output is not a valid WebP file");
			}
			if (rasterized != null) {
				Files.deleteIfExists(rasterized);
			}
			Dimensions dimensions = targetDimensions(inspection, imageType);
			return new ConvertedImage(output, dimensions.width(), dimensions.height());
		} catch (IOException e) {
			throw new ImageProcessingException("IMAGE_TOOL_MISSING", "Image conversion executable is missing or inaccessible", e);
		}
	}

	private List<String> cwebpCommand(Path input, Path output, ImageInspection inspection, ImageType imageType) {
		List<String> command = new ArrayList<>();
		command.add(properties.getImage().getCwebpPath());
		command.add("-quiet");
		command.add("-metadata");
		command.add("none");
		if (imageType == ImageType.LOGO) {
			command.add("-lossless");
		} else {
			command.add("-q");
			command.add(String.valueOf(quality(imageType)));
			Dimensions dimensions = targetDimensions(inspection, imageType);
			if (shouldResize(inspection, dimensions)) {
				command.add("-resize");
				command.add(String.valueOf(dimensions.width()));
				command.add(String.valueOf(dimensions.height()));
			}
		}
		command.add(input.toString());
		command.add("-o");
		command.add(output.toString());
		return command;
	}

	private List<String> gifCommand(Path input, Path output, ImageType imageType) {
		return List.of(properties.getImage().getGif2webpPath(), "-quiet", "-q", String.valueOf(quality(imageType)),
				input.toString(), "-o", output.toString());
	}

	private void run(List<String> command, String missingToolCode) {
		try {
			ProcessRunner.ProcessResult result = processRunner.run(command, Duration.ofMinutes(2));
			if (result.exitCode() == 124) {
				throw new ImageProcessingException("IMAGE_CONVERSION_TIMEOUT", "Image conversion timed out");
			}
			if (result.exitCode() != 0) {
				throw new ImageProcessingException("IMAGE_CONVERSION_FAILED", trim(result.stderr()));
			}
		} catch (IOException e) {
			throw new ImageProcessingException(missingToolCode, "Image conversion executable is missing", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ImageProcessingException("IMAGE_CONVERSION_INTERRUPTED", "Image conversion interrupted", e);
		}
	}

	private int quality(ImageType imageType) {
		if (imageType == ImageType.POSTER) {
			return properties.getImage().getPosterQuality();
		}
		return properties.getImage().getStaticQuality();
	}

	private Dimensions targetDimensions(ImageInspection inspection, ImageType imageType) {
		if (inspection.width() <= 0 || inspection.height() <= 0 || imageType == ImageType.LOGO) {
			return new Dimensions(inspection.width(), inspection.height());
		}
		int maxLongEdge = switch (imageType) {
			case POSTER -> properties.getImage().getPosterMaxLongEdge();
			case BACKDROP -> properties.getImage().getBackdropMaxLongEdge();
			case SCENE -> properties.getImage().getSceneMaxLongEdge();
			case LOGO -> Math.max(inspection.width(), inspection.height());
		};
		int currentLongEdge = Math.max(inspection.width(), inspection.height());
		if (currentLongEdge <= maxLongEdge) {
			return new Dimensions(inspection.width(), inspection.height());
		}
		double scale = (double) maxLongEdge / currentLongEdge;
		return new Dimensions(Math.max(1, (int) Math.round(inspection.width() * scale)),
				Math.max(1, (int) Math.round(inspection.height() * scale)));
	}

	private boolean shouldResize(ImageInspection inspection, Dimensions dimensions) {
		return inspection.width() > 0 && inspection.height() > 0
				&& (inspection.width() != dimensions.width() || inspection.height() != dimensions.height());
	}

	private boolean looksLikeWebp(Path output) throws IOException {
		byte[] header = new byte[12];
		try (var input = Files.newInputStream(output)) {
			int read = input.read(header);
			if (read < 12) {
				return false;
			}
		}
		if (header.length < 12) {
			return false;
		}
		return "RIFF".equals(new String(header, 0, 4, StandardCharsets.US_ASCII))
				&& "WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII));
	}

	private String trim(String value) {
		if (value == null) {
			return "";
		}
		return value.length() > 1000 ? value.substring(0, 1000) : value;
	}

	private record Dimensions(int width, int height) {
	}
}
