package com.ssafy.pilgrimage.batch.ingest.job;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.ssafy.pilgrimage.batch.ingest.config.PilgrimageProperties;
import com.ssafy.pilgrimage.batch.ingest.crawl.TistoryPostDiscoverer;
import com.ssafy.pilgrimage.batch.ingest.crawl.TistoryPostHtmlFetcher;
import com.ssafy.pilgrimage.batch.ingest.crawl.TistoryPostParser;
import com.ssafy.pilgrimage.batch.ingest.geocode.SceneSyncService;
import com.ssafy.pilgrimage.batch.ingest.image.ImageUploadService;
import com.ssafy.pilgrimage.batch.ingest.model.CrawlPostRow;
import com.ssafy.pilgrimage.batch.ingest.model.CrawlSceneRow;
import com.ssafy.pilgrimage.batch.ingest.model.ImageIngestTaskRow;
import com.ssafy.pilgrimage.batch.ingest.model.ParsedPost;
import com.ssafy.pilgrimage.batch.ingest.persistence.PilgrimageIngestRepository;
import com.ssafy.pilgrimage.batch.ingest.support.Hashing;
import com.ssafy.pilgrimage.batch.ingest.tmdb.TmdbSyncService;

@Configuration
public class PilgrimageIngestJobConfig {

	private static final Logger log = LoggerFactory.getLogger(PilgrimageIngestJobConfig.class);

	@Bean
	public Job pilgrimageIngestJob(JobRepository jobRepository, Step discoverPostsStep, Step parsePostsStep,
			Step syncDramasStep, Step syncScenesStep, Step uploadImagesStep, Step summarizeIngestStep) {
		return new JobBuilder("pilgrimageIngestJob", jobRepository)
				.start(discoverPostsStep)
				.next(parsePostsStep)
				.next(syncDramasStep)
				.next(syncScenesStep)
				.next(uploadImagesStep)
				.next(summarizeIngestStep)
				.build();
	}

	@Bean
	public Step discoverPostsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			TistoryPostDiscoverer discoverer, PilgrimageIngestRepository repository, PilgrimageProperties properties) {
		return new StepBuilder("discoverPostsStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					int page = 1;
					int maxPages = properties.getCrawler().getMaxPages();
					while (maxPages <= 0 || page <= maxPages) {
						var discoveryPage = discoverer.discoverPage(page);
						int newPosts = 0;
						for (var post : discoveryPage.posts()) {
							if (!repository.existsPost(post.postKey())) {
								newPosts++;
							}
							repository.upsertDiscoveredPost(post);
							log.debug("postKey={} status=DISCOVERED", Hashing.hex(post.postKey()));
						}
						if (!discoveryPage.hasNext() || newPosts == 0) {
							break;
						}
						discoverer.delayBetweenRequests();
						page++;
					}
					return org.springframework.batch.repeat.RepeatStatus.FINISHED;
				}, new ResourcelessTransactionManager())
				.build();
	}

	@Bean
	public Step parsePostsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			ListItemReader<CrawlPostRow> parsePostReader, ItemProcessor<CrawlPostRow, ParsedPost> parsePostProcessor,
			ItemWriter<ParsedPost> parsePostWriter) {
		return new StepBuilder("parsePostsStep", jobRepository)
				.<CrawlPostRow, ParsedPost>chunk(1, transactionManager)
				.reader(parsePostReader)
				.processor(parsePostProcessor)
				.writer(parsePostWriter)
				.build();
	}

	@Bean
	@StepScope
	public ListItemReader<CrawlPostRow> parsePostReader(PilgrimageIngestRepository repository,
			@Value("#{jobParameters['retryFailed']}") String retryFailed) {
		return new ListItemReader<>(repository.findPostsForParsing(bool(retryFailed, true)));
	}

	@Bean
	@StepScope
	public ItemProcessor<CrawlPostRow, ParsedPost> parsePostProcessor(TistoryPostHtmlFetcher fetcher,
			TistoryPostParser parser, PilgrimageIngestRepository repository) {
		return post -> {
			try {
				String html = fetcher.fetch(post.postUrl());
				return parser.parse(post, html);
			} catch (RuntimeException e) {
				repository.markPostFailure(post.postKey(), "POST_PARSE_FAILED", e.getMessage());
				return null;
			}
		};
	}

	@Bean
	public ItemWriter<ParsedPost> parsePostWriter(PilgrimageIngestRepository repository) {
		return chunk -> {
			for (ParsedPost post : chunk) {
				repository.saveParsedPost(post);
			}
		};
	}

	@Bean
	public Step syncDramasStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			PilgrimageIngestRepository repository, TmdbSyncService tmdbSyncService) {
		return new StepBuilder("syncDramasStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					boolean retryFailed = bool((String) chunkContext.getStepContext().getJobParameters().get("retryFailed"), true);
					for (CrawlPostRow post : repository.findPostsForTmdb(retryFailed)) {
						tmdbSyncService.process(post);
					}
					return org.springframework.batch.repeat.RepeatStatus.FINISHED;
				}, new ResourcelessTransactionManager())
				.build();
	}

	@Bean
	public Step syncScenesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			PilgrimageIngestRepository repository, SceneSyncService sceneSyncService) {
		return new StepBuilder("syncScenesStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					boolean retryFailed = bool((String) chunkContext.getStepContext().getJobParameters().get("retryFailed"), true);
					for (CrawlSceneRow scene : repository.findScenesForSync(retryFailed)) {
						sceneSyncService.process(scene);
					}
					return org.springframework.batch.repeat.RepeatStatus.FINISHED;
				}, new ResourcelessTransactionManager())
				.build();
	}

	@Bean
	public Step uploadImagesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			ListItemReader<ImageIngestTaskRow> imageTaskReader, ItemProcessor<ImageIngestTaskRow, ImageIngestTaskRow> imageTaskProcessor,
			ItemWriter<ImageIngestTaskRow> imageTaskWriter) {
		return new StepBuilder("uploadImagesStep", jobRepository)
				.<ImageIngestTaskRow, ImageIngestTaskRow>chunk(1, transactionManager)
				.reader(imageTaskReader)
				.processor(imageTaskProcessor)
				.writer(imageTaskWriter)
				.build();
	}

	@Bean
	@StepScope
	public ListItemReader<ImageIngestTaskRow> imageTaskReader(PilgrimageIngestRepository repository,
			@Value("#{jobParameters['retryFailed']}") String retryFailed) {
		return new ListItemReader<>(repository.findImageTasksForUpload(bool(retryFailed, true)));
	}

	@Bean
	@StepScope
	public ItemProcessor<ImageIngestTaskRow, ImageIngestTaskRow> imageTaskProcessor(ImageUploadService imageUploadService) {
		return task -> {
			imageUploadService.process(task);
			return null;
		};
	}

	@Bean
	public ItemWriter<ImageIngestTaskRow> imageTaskWriter() {
		return chunk -> {
		};
	}

	@Bean
	public Step summarizeIngestStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			PilgrimageIngestRepository repository) {
		return new StepBuilder("summarizeIngestStep", jobRepository)
				.tasklet((contribution, chunkContext) -> {
					Map<String, Object> summary = repository.summarize();
					log.info("pilgrimageIngestSummary={}", summary);
					return org.springframework.batch.repeat.RepeatStatus.FINISHED;
				}, new ResourcelessTransactionManager())
				.build();
	}

	private static boolean bool(String value, boolean defaultValue) {
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		return Boolean.parseBoolean(value);
	}
}
