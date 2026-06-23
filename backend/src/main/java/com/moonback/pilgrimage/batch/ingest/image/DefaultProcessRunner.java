package com.moonback.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class DefaultProcessRunner implements ProcessRunner {

	@Override
	public ProcessResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
		boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
		if (!finished) {
			process.destroyForcibly();
			return new ProcessResult(124, "Process timed out");
		}
		String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
		return new ProcessResult(process.exitValue(), stderr);
	}
}
