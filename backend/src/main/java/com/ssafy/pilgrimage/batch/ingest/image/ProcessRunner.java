package com.ssafy.pilgrimage.batch.ingest.image;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import lombok.Builder;

public interface ProcessRunner {
	ProcessResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;

	@Builder
	record ProcessResult(int exitCode, String stderr) {
	}
}
