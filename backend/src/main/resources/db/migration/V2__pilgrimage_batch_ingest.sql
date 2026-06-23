CREATE TABLE IF NOT EXISTS `Drama` (
  `drama_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `original_title` VARCHAR(255) NULL,
  `original_language` VARCHAR(20) NULL,
  `origin_country` VARCHAR(255) NULL,
  `released_at` DATE NULL,
  `description` TEXT NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `Genre` (
  `genre_id` INT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`genre_id`),
  UNIQUE KEY `uk_genre_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `DramaGenre` (
  `drama_id` INT NOT NULL,
  `genre_id` INT NOT NULL,
  PRIMARY KEY (`drama_id`, `genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `DramaImg` (
  `img_id` INT NOT NULL AUTO_INCREMENT,
  `drama_id` INT NOT NULL,
  `url` VARCHAR(1000) NOT NULL,
  `image_type` VARCHAR(20) NULL,
  `object_key` VARCHAR(1000) NULL,
  `source_url` VARCHAR(1000) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `content_hash` BINARY(32) NULL,
  `width` INT NULL,
  `height` INT NULL,
  PRIMARY KEY (`img_id`),
  UNIQUE KEY `uk_drama_img_url` (`drama_id`, `url`),
  UNIQUE KEY `uk_drama_img_content` (`drama_id`, `image_type`, `content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `Scene` (
  `scene_id` INT NOT NULL AUTO_INCREMENT,
  `drama_id` INT NOT NULL,
  `name` VARCHAR(255) NULL,
  `description` VARCHAR(255) NULL,
  `address` VARCHAR(255) NULL,
  `latitude` DOUBLE NULL,
  `longitude` DOUBLE NULL,
  `import_key` BINARY(32) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`scene_id`),
  UNIQUE KEY `uk_scene_import_key` (`import_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `SceneImg` (
  `img_id` INT NOT NULL AUTO_INCREMENT,
  `scene_id` INT NOT NULL,
  `url` VARCHAR(1000) NOT NULL,
  `object_key` VARCHAR(1000) NULL,
  `source_url` VARCHAR(1000) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `content_hash` BINARY(32) NULL,
  `width` INT NULL,
  `height` INT NULL,
  PRIMARY KEY (`img_id`),
  UNIQUE KEY `uk_scene_img_content` (`scene_id`, `content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DELIMITER $$

DROP PROCEDURE IF EXISTS pilgrimage_add_column_if_missing $$
CREATE PROCEDURE pilgrimage_add_column_if_missing(
  IN table_name_arg VARCHAR(64),
  IN column_name_arg VARCHAR(64),
  IN column_definition_arg TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_arg
      AND COLUMN_NAME = column_name_arg
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` ADD COLUMN ', column_definition_arg);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS pilgrimage_modify_column_if_exists $$
CREATE PROCEDURE pilgrimage_modify_column_if_exists(
  IN table_name_arg VARCHAR(64),
  IN column_name_arg VARCHAR(64),
  IN column_definition_arg TEXT
)
BEGIN
  IF EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_arg
      AND COLUMN_NAME = column_name_arg
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` MODIFY COLUMN ', column_definition_arg);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS pilgrimage_add_index_if_missing $$
CREATE PROCEDURE pilgrimage_add_index_if_missing(
  IN table_name_arg VARCHAR(64),
  IN index_name_arg VARCHAR(64),
  IN index_definition_arg TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_arg
      AND INDEX_NAME = index_name_arg
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` ADD ', index_definition_arg);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DROP PROCEDURE IF EXISTS pilgrimage_add_fk_if_missing $$
CREATE PROCEDURE pilgrimage_add_fk_if_missing(
  IN table_name_arg VARCHAR(64),
  IN constraint_name_arg VARCHAR(64),
  IN fk_definition_arg TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_arg
      AND CONSTRAINT_NAME = constraint_name_arg
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` ADD CONSTRAINT `', constraint_name_arg, '` ', fk_definition_arg);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$

DELIMITER ;

CALL pilgrimage_add_column_if_missing('Drama', 'original_title', '`original_title` VARCHAR(255) NULL');
CALL pilgrimage_add_column_if_missing('Drama', 'original_language', '`original_language` VARCHAR(20) NULL');
CALL pilgrimage_add_column_if_missing('Drama', 'origin_country', '`origin_country` VARCHAR(255) NULL');
CALL pilgrimage_add_column_if_missing('Drama', 'updated_at', '`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL pilgrimage_modify_column_if_exists('Drama', 'released_at', '`released_at` DATE NULL');

CALL pilgrimage_modify_column_if_exists('DramaImg', 'url', '`url` VARCHAR(1000) NOT NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'image_type', '`image_type` VARCHAR(20) NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'object_key', '`object_key` VARCHAR(1000) NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'source_url', '`source_url` VARCHAR(1000) NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'sort_order', '`sort_order` INT NOT NULL DEFAULT 0');
CALL pilgrimage_add_column_if_missing('DramaImg', 'content_hash', '`content_hash` BINARY(32) NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'width', '`width` INT NULL');
CALL pilgrimage_add_column_if_missing('DramaImg', 'height', '`height` INT NULL');
CALL pilgrimage_add_index_if_missing('DramaImg', 'uk_drama_img_content', 'UNIQUE KEY `uk_drama_img_content` (`drama_id`, `image_type`, `content_hash`)');

CALL pilgrimage_add_index_if_missing('Genre', 'uk_genre_name', 'UNIQUE KEY `uk_genre_name` (`name`)');

CALL pilgrimage_add_column_if_missing('Scene', 'import_key', '`import_key` BINARY(32) NULL');
CALL pilgrimage_add_column_if_missing('Scene', 'created_at', '`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP');
CALL pilgrimage_add_column_if_missing('Scene', 'updated_at', '`updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL pilgrimage_add_index_if_missing('Scene', 'uk_scene_import_key', 'UNIQUE KEY `uk_scene_import_key` (`import_key`)');

CALL pilgrimage_modify_column_if_exists('SceneImg', 'url', '`url` VARCHAR(1000) NOT NULL');
CALL pilgrimage_add_column_if_missing('SceneImg', 'object_key', '`object_key` VARCHAR(1000) NULL');
CALL pilgrimage_add_column_if_missing('SceneImg', 'source_url', '`source_url` VARCHAR(1000) NULL');
CALL pilgrimage_add_column_if_missing('SceneImg', 'sort_order', '`sort_order` INT NOT NULL DEFAULT 0');
CALL pilgrimage_add_column_if_missing('SceneImg', 'content_hash', '`content_hash` BINARY(32) NULL');
CALL pilgrimage_add_column_if_missing('SceneImg', 'width', '`width` INT NULL');
CALL pilgrimage_add_column_if_missing('SceneImg', 'height', '`height` INT NULL');
CALL pilgrimage_add_index_if_missing('SceneImg', 'uk_scene_img_content', 'UNIQUE KEY `uk_scene_img_content` (`scene_id`, `content_hash`)');

CALL pilgrimage_add_fk_if_missing('DramaImg', 'fk_drama_img_drama', 'FOREIGN KEY (`drama_id`) REFERENCES `Drama` (`drama_id`)');
CALL pilgrimage_add_fk_if_missing('DramaGenre', 'fk_drama_genre_drama', 'FOREIGN KEY (`drama_id`) REFERENCES `Drama` (`drama_id`)');
CALL pilgrimage_add_fk_if_missing('DramaGenre', 'fk_drama_genre_genre', 'FOREIGN KEY (`genre_id`) REFERENCES `Genre` (`genre_id`)');
CALL pilgrimage_add_fk_if_missing('Scene', 'fk_scene_drama', 'FOREIGN KEY (`drama_id`) REFERENCES `Drama` (`drama_id`)');
CALL pilgrimage_add_fk_if_missing('SceneImg', 'fk_scene_img_scene', 'FOREIGN KEY (`scene_id`) REFERENCES `Scene` (`scene_id`)');

DROP PROCEDURE IF EXISTS pilgrimage_add_column_if_missing;
DROP PROCEDURE IF EXISTS pilgrimage_modify_column_if_exists;
DROP PROCEDURE IF EXISTS pilgrimage_add_index_if_missing;
DROP PROCEDURE IF EXISTS pilgrimage_add_fk_if_missing;

CREATE TABLE IF NOT EXISTS `CrawlPost` (
  `post_key` BINARY(32) NOT NULL,
  `post_url` VARCHAR(1000) NOT NULL,
  `post_title` VARCHAR(500) NULL,
  `published_at` DATETIME NULL,
  `drama_query` VARCHAR(255) NULL,
  `normalized_query` VARCHAR(255) NULL,
  `tmdb_id` INT NULL,
  `content_hash` BINARY(32) NULL,
  `status` VARCHAR(40) NOT NULL,
  `error_code` VARCHAR(80) NULL,
  `error_message` VARCHAR(1000) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_crawled_at` DATETIME NULL,
  PRIMARY KEY (`post_key`),
  UNIQUE KEY `uk_crawl_post_url` (`post_url`(768)),
  KEY `idx_crawl_post_status` (`status`),
  KEY `idx_crawl_post_tmdb` (`tmdb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `CrawlScene` (
  `source_key` BINARY(32) NOT NULL,
  `post_key` BINARY(32) NOT NULL,
  `source_order` INT NOT NULL,
  `same_name_occurrence` INT NOT NULL,
  `raw_name` VARCHAR(255) NULL,
  `raw_text` TEXT NULL,
  `raw_address` VARCHAR(1000) NULL,
  `image_urls` JSON NULL,
  `scene_id` INT NULL,
  `status` VARCHAR(40) NOT NULL,
  `error_code` VARCHAR(80) NULL,
  `error_message` VARCHAR(1000) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`source_key`),
  UNIQUE KEY `uk_crawl_scene_post_order` (`post_key`, `source_order`),
  KEY `idx_crawl_scene_status` (`status`),
  KEY `idx_crawl_scene_scene` (`scene_id`),
  CONSTRAINT `fk_crawl_scene_post` FOREIGN KEY (`post_key`) REFERENCES `CrawlPost` (`post_key`),
  CONSTRAINT `fk_crawl_scene_scene` FOREIGN KEY (`scene_id`) REFERENCES `Scene` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ImageImportTask` (
  `task_key` BINARY(32) NOT NULL,
  `owner_type` VARCHAR(20) NOT NULL,
  `owner_id` INT NOT NULL,
  `image_type` VARCHAR(20) NOT NULL,
  `source_url` VARCHAR(1000) NOT NULL,
  `source_identity` VARCHAR(1000) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `object_key` VARCHAR(1000) NULL,
  `content_hash` BINARY(32) NULL,
  `width` INT NULL,
  `height` INT NULL,
  `status` VARCHAR(40) NOT NULL,
  `attempt_count` INT NOT NULL DEFAULT 0,
  `reused_existing_object` BOOLEAN NOT NULL DEFAULT FALSE,
  `error_code` VARCHAR(80) NULL,
  `error_message` VARCHAR(1000) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_key`),
  KEY `idx_image_task_status` (`status`),
  KEY `idx_image_task_owner` (`owner_type`, `owner_id`),
  KEY `idx_image_task_content` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `DramaMatchOverride` (
  `normalized_title` VARCHAR(255) NOT NULL,
  `tmdb_id` INT NOT NULL,
  `memo` VARCHAR(1000) NULL,
  PRIMARY KEY (`normalized_title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `GeocodeCache` (
  `query_hash` BINARY(32) NOT NULL,
  `query_text` VARCHAR(1000) NOT NULL,
  `status` VARCHAR(40) NOT NULL,
  `canonical_address` VARCHAR(1000) NULL,
  `region_1depth` VARCHAR(100) NULL,
  `region_2depth` VARCHAR(100) NULL,
  `latitude` DOUBLE NULL,
  `longitude` DOUBLE NULL,
  `response_json` JSON NULL,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`query_hash`),
  KEY `idx_geocode_cache_status_updated` (`status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_INSTANCE` (
  `JOB_INSTANCE_ID` BIGINT NOT NULL PRIMARY KEY,
  `VERSION` BIGINT NULL,
  `JOB_NAME` VARCHAR(100) NOT NULL,
  `JOB_KEY` VARCHAR(32) NOT NULL,
  CONSTRAINT `JOB_INST_UN` UNIQUE (`JOB_NAME`, `JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION` (
  `JOB_EXECUTION_ID` BIGINT NOT NULL PRIMARY KEY,
  `VERSION` BIGINT NULL,
  `JOB_INSTANCE_ID` BIGINT NOT NULL,
  `CREATE_TIME` DATETIME(6) NOT NULL,
  `START_TIME` DATETIME(6) DEFAULT NULL,
  `END_TIME` DATETIME(6) DEFAULT NULL,
  `STATUS` VARCHAR(10) NULL,
  `EXIT_CODE` VARCHAR(2500) NULL,
  `EXIT_MESSAGE` VARCHAR(2500) NULL,
  `LAST_UPDATED` DATETIME(6) NULL,
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_PARAMS` (
  `JOB_EXECUTION_ID` BIGINT NOT NULL,
  `PARAMETER_NAME` VARCHAR(100) NOT NULL,
  `PARAMETER_TYPE` VARCHAR(100) NOT NULL,
  `PARAMETER_VALUE` VARCHAR(2500) NULL,
  `IDENTIFYING` CHAR(1) NOT NULL,
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION` (
  `STEP_EXECUTION_ID` BIGINT NOT NULL PRIMARY KEY,
  `VERSION` BIGINT NOT NULL,
  `STEP_NAME` VARCHAR(100) NOT NULL,
  `JOB_EXECUTION_ID` BIGINT NOT NULL,
  `CREATE_TIME` DATETIME(6) NOT NULL,
  `START_TIME` DATETIME(6) DEFAULT NULL,
  `END_TIME` DATETIME(6) DEFAULT NULL,
  `STATUS` VARCHAR(10) NULL,
  `COMMIT_COUNT` BIGINT NULL,
  `READ_COUNT` BIGINT NULL,
  `FILTER_COUNT` BIGINT NULL,
  `WRITE_COUNT` BIGINT NULL,
  `READ_SKIP_COUNT` BIGINT NULL,
  `WRITE_SKIP_COUNT` BIGINT NULL,
  `PROCESS_SKIP_COUNT` BIGINT NULL,
  `ROLLBACK_COUNT` BIGINT NULL,
  `EXIT_CODE` VARCHAR(2500) NULL,
  `EXIT_MESSAGE` VARCHAR(2500) NULL,
  `LAST_UPDATED` DATETIME(6) NULL,
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION_CONTEXT` (
  `STEP_EXECUTION_ID` BIGINT NOT NULL PRIMARY KEY,
  `SHORT_CONTEXT` VARCHAR(2500) NOT NULL,
  `SERIALIZED_CONTEXT` TEXT NULL,
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_CONTEXT` (
  `JOB_EXECUTION_ID` BIGINT NOT NULL PRIMARY KEY,
  `SHORT_CONTEXT` VARCHAR(2500) NOT NULL,
  `SERIALIZED_CONTEXT` TEXT NULL,
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_STEP_EXECUTION_SEQ` (
  `ID` BIGINT NOT NULL,
  `UNIQUE_KEY` CHAR(1) NOT NULL,
  CONSTRAINT `UNIQUE_KEY_UN` UNIQUE (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_EXECUTION_SEQ` (
  `ID` BIGINT NOT NULL,
  `UNIQUE_KEY` CHAR(1) NOT NULL,
  CONSTRAINT `UNIQUE_KEY_UN` UNIQUE (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `BATCH_JOB_SEQ` (
  `ID` BIGINT NOT NULL,
  `UNIQUE_KEY` CHAR(1) NOT NULL,
  CONSTRAINT `UNIQUE_KEY_UN` UNIQUE (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO `BATCH_STEP_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');
INSERT IGNORE INTO `BATCH_JOB_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');
INSERT IGNORE INTO `BATCH_JOB_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');
