package com.ssafy.pilgrimage.batch.ingest.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.model.ImageType;

class ExternalProcessWebpConverterTest {

	@TempDir
	Path tempDir;

	@Test
	void resizesToMaxLongEdgeWithoutExternalBinary() throws Exception {
		PilgrimageProperties properties = new PilgrimageProperties();
		properties.getImage().setTempDir(tempDir);
		CapturingRunner runner = new CapturingRunner();
		ExternalProcessWebpConverter converter = new ExternalProcessWebpConverter(properties, runner);
		Path input = Files.writeString(tempDir.resolve("input.png"), "not-used");

		ConvertedImage converted = converter.convert(input, new ImageInspection(ImageFormat.PNG, 3000, 1500),
				ImageType.BACKDROP);

		assertThat(converted.width()).isEqualTo(1920);
		assertThat(converted.height()).isEqualTo(960);
		assertThat(runner.commands.getFirst()).contains("-resize", "1920", "960");
		assertThat(Files.exists(converted.path())).isTrue();
	}

	private static class CapturingRunner implements ProcessRunner {
		private final List<List<String>> commands = new ArrayList<>();

		@Override
		public ProcessResult run(List<String> command, Duration timeout) throws java.io.IOException {
			commands.add(command);
			int outputFlag = command.indexOf("-o");
			Path output = Path.of(command.get(outputFlag + 1));
			Files.write(output, "RIFFxxxxWEBP".getBytes(StandardCharsets.US_ASCII));
			return new ProcessResult(0, "");
		}
	}
}
