CREATE DATABASE IF NOT EXISTS `moonbackdb`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `moonbackdb`;

-- DDL-only database bootstrap generated from DBSchema.sql.sql and src/main/resources/db/migration.
-- Data INSERT statements, table locks, and dump session metadata are omitted.
-- Run this script after selecting the target database.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Service tables used by the application API (batch tables excluded).

DROP TABLE IF EXISTS `memberrole`;
CREATE TABLE `memberrole` (
  `role_id` int NOT NULL,
  `role_name` varchar(50) NOT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_member_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `memberstatus`;
CREATE TABLE `memberstatus` (
  `status_id` int NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`status_id`),
  UNIQUE KEY `uk_member_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `member`;
CREATE TABLE `member` (
  `member_id` int NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(255) NOT NULL,
  `role_id` int NOT NULL,
  `status_id` int NOT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uk_member_email` (`email`),
  CONSTRAINT `fk_member_role` FOREIGN KEY (`role_id`) REFERENCES `memberrole` (`role_id`),
  CONSTRAINT `fk_member_status` FOREIGN KEY (`status_id`) REFERENCES `memberstatus` (`status_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `drama`;
CREATE TABLE `drama` (
  `drama_id` int NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_language` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin_country` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `released_at` date DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `genre`;
CREATE TABLE `genre` (
  `genre_id` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`genre_id`),
  UNIQUE KEY `uk_genre_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `dramagenre`;
CREATE TABLE `dramagenre` (
  `drama_id` int NOT NULL,
  `genre_id` int NOT NULL,
  PRIMARY KEY (`drama_id`,`genre_id`),
  KEY `fk_drama_genre_genre` (`genre_id`),
  CONSTRAINT `fk_drama_genre_drama` FOREIGN KEY (`drama_id`) REFERENCES `drama` (`drama_id`),
  CONSTRAINT `fk_drama_genre_genre` FOREIGN KEY (`genre_id`) REFERENCES `genre` (`genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `dramaimg`;
CREATE TABLE `dramaimg` (
  `img_id` int NOT NULL AUTO_INCREMENT,
  `drama_id` int NOT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `object_key` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `content_hash` binary(32) DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  PRIMARY KEY (`img_id`),
  UNIQUE KEY `uk_drama_img_content` (`drama_id`,`image_type`,`content_hash`),
  CONSTRAINT `fk_drama_img_drama` FOREIGN KEY (`drama_id`) REFERENCES `drama` (`drama_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4407 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `scene`;
CREATE TABLE `scene` (
  `scene_id` int NOT NULL AUTO_INCREMENT,
  `drama_id` int NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `import_key` binary(32) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`scene_id`),
  UNIQUE KEY `uk_scene_import_key` (`import_key`),
  KEY `fk_scene_drama` (`drama_id`),
  CONSTRAINT `fk_scene_drama` FOREIGN KEY (`drama_id`) REFERENCES `drama` (`drama_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2755 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sceneimg`;
CREATE TABLE `sceneimg` (
  `img_id` int NOT NULL AUTO_INCREMENT,
  `scene_id` int NOT NULL,
  `url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_key` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `content_hash` binary(32) DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  PRIMARY KEY (`img_id`),
  UNIQUE KEY `uk_scene_img_content` (`scene_id`,`content_hash`),
  CONSTRAINT `fk_scene_img_scene` FOREIGN KEY (`scene_id`) REFERENCES `scene` (`scene_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8864 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `contenttype`;
CREATE TABLE `contenttype` (
  `content_type_id` int NOT NULL,
  `name` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`content_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `place`;
CREATE TABLE `place` (
  `place_id` int NOT NULL AUTO_INCREMENT,
  `content_id` varchar(255) NOT NULL,
  `content_type_id` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` varchar(255) NOT NULL,
  `latitude` double NOT NULL,
  `longitude` double NOT NULL,
  `description` text,
  `src_created_at` datetime NOT NULL,
  `src_updated_at` datetime NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`place_id`),
  UNIQUE KEY `uk_place_content_id` (`content_id`),
  CONSTRAINT `fk_place_content_type` FOREIGN KEY (`content_type_id`) REFERENCES `contenttype` (`content_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=65536 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `placeimg`;
CREATE TABLE `placeimg` (
  `img_id` int NOT NULL AUTO_INCREMENT,
  `place_id` int NOT NULL,
  `url` varchar(1000) NOT NULL,
  PRIMARY KEY (`img_id`),
  KEY `fk_place_img_place` (`place_id`),
  CONSTRAINT `fk_place_img_place` FOREIGN KEY (`place_id`) REFERENCES `place` (`place_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=131071 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `travelplan`;
CREATE TABLE `travelplan` (
  `plan_id` int NOT NULL AUTO_INCREMENT,
  `member_id` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `begin_date` date NOT NULL,
  `end_date` date NOT NULL,
  `memo` text,
  PRIMARY KEY (`plan_id`),
  CONSTRAINT `ck_travelplan_date` CHECK ((`begin_date` <= `end_date`)),
  CONSTRAINT `fk_travel_plan_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `plandetail`;
CREATE TABLE `plandetail` (
  `detail_id` int NOT NULL AUTO_INCREMENT,
  `plan_id` int NOT NULL,
  `place_id` int DEFAULT NULL,
  `scene_id` int DEFAULT NULL,
  `day_no` int NOT NULL,
  `begin_time` time NOT NULL,
  PRIMARY KEY (`detail_id`),
  CONSTRAINT `ck_plandetail_day` CHECK ((`day_no` >= 1)),
  CONSTRAINT `place_xor` CHECK ((((`place_id` is not null) and (`scene_id` is null)) or ((`place_id` is null) and (`scene_id` is not null)))),
  CONSTRAINT `fk_plan_detail_plan` FOREIGN KEY (`plan_id`) REFERENCES `travelplan` (`plan_id`),
  CONSTRAINT `fk_plan_detail_place` FOREIGN KEY (`place_id`) REFERENCES `place` (`place_id`),
  CONSTRAINT `fk_plan_detail_scene` FOREIGN KEY (`scene_id`) REFERENCES `scene` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `wishlist`;
CREATE TABLE `wishlist` (
  `wishlist_id` int NOT NULL AUTO_INCREMENT,
  `member_id` int NOT NULL,
  `scene_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`wishlist_id`),
  UNIQUE KEY `uk_wishlist_member_id` (`member_id`,`scene_id`),
  CONSTRAINT `fk_wishlist_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`),
  CONSTRAINT `fk_wishlist_scene` FOREIGN KEY (`scene_id`) REFERENCES `scene` (`scene_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Static reference data required by service foreign keys.
INSERT INTO `memberrole` (`role_id`, `role_name`) VALUES
  (1, 'USER'),
  (2, 'ADMIN');

INSERT INTO `memberstatus` (`status_id`, `status`) VALUES
  (1, 'ACTIVE'),
  (2, 'WITHDRAWN'),
  (3, 'SUSPENDED');

INSERT INTO `contenttype` (`content_type_id`, `name`) VALUES
  (12, '관광지'),
  (14, '문화시설'),
  (15, '축제공연행사'),
  (25, '여행코스'),
  (28, '레포츠'),
  (32, '숙박'),
  (38, '쇼핑'),
  (39, '관광지');

-- Source: DBSchema.sql.sql (table definitions only)
DROP TABLE IF EXISTS `attractions`;
CREATE TABLE `attractions` (
  `no` int NOT NULL AUTO_INCREMENT COMMENT '명소코드',
  `content_id` int DEFAULT NULL COMMENT '콘텐츠번호',
  `title` varchar(500) DEFAULT NULL COMMENT '명소이름',
  `content_type_id` int DEFAULT NULL COMMENT '콘텐츠타입',
  `area_code` int DEFAULT NULL COMMENT '시도코드',
  `si_gun_gu_code` int DEFAULT NULL COMMENT '구군코드',
  `first_image1` varchar(100) DEFAULT NULL COMMENT '이미지경로1',
  `first_image2` varchar(100) DEFAULT NULL COMMENT '이미지경로2',
  `map_level` int DEFAULT NULL COMMENT '줌레벨',
  `latitude` decimal(20,17) DEFAULT NULL COMMENT '위도',
  `longitude` decimal(20,17) DEFAULT NULL COMMENT '경도',
  `tel` varchar(20) DEFAULT NULL COMMENT '전화번호',
  `addr1` varchar(100) DEFAULT NULL COMMENT '주소1',
  `addr2` varchar(100) DEFAULT NULL COMMENT '주소2',
  `homepage` varchar(1000) DEFAULT NULL COMMENT '홈페이지',
  `overview` varchar(10000) DEFAULT NULL COMMENT '설명',
  PRIMARY KEY (`no`),
  KEY `attractions_typeid_to_types_typeid_fk_idx` (`content_type_id`),
  KEY `attractions_sido_to_sidos_code_fk_idx` (`area_code`),
  KEY `attractions_sigungu_to_guguns_gugun_fk_idx` (`si_gun_gu_code`),
  CONSTRAINT `attractions_area_to_sidos_code_fk` FOREIGN KEY (`area_code`) REFERENCES `sidos` (`sido_code`),
  CONSTRAINT `attractions_sigungu_to_guguns_gugun_fk` FOREIGN KEY (`si_gun_gu_code`) REFERENCES `guguns` (`gugun_code`),
  CONSTRAINT `attractions_typeid_to_types_typeid_fk` FOREIGN KEY (`content_type_id`) REFERENCES `contenttypes` (`content_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=107559 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='명소정보테이블';

DROP TABLE IF EXISTS `contenttypes`;
CREATE TABLE `contenttypes` (
  `content_type_id` int NOT NULL COMMENT '콘텐츠타입번호',
  `content_type_name` varchar(45) DEFAULT NULL COMMENT '콘텐츠타입이름',
  PRIMARY KEY (`content_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='콘텐츠타입정보테이블';

DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS `geocodecache`;
CREATE TABLE `geocodecache` (
  `query_hash` binary(32) NOT NULL,
  `query_text` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `canonical_address` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `region_1depth` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `region_2depth` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `response_json` json DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`query_hash`),
  KEY `idx_geocode_cache_status_updated` (`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `guguns`;
CREATE TABLE `guguns` (
  `no` int NOT NULL AUTO_INCREMENT COMMENT '구군번호',
  `sido_code` int NOT NULL COMMENT '시도코드',
  `gugun_code` int NOT NULL COMMENT '구군코드',
  `gugun_name` varchar(20) DEFAULT NULL COMMENT '구군이름',
  PRIMARY KEY (`no`),
  KEY `guguns_sido_to_sidos_cdoe_fk_idx` (`sido_code`),
  KEY `gugun_code_idx` (`gugun_code`),
  CONSTRAINT `guguns_sido_to_sidos_cdoe_fk` FOREIGN KEY (`sido_code`) REFERENCES `sidos` (`sido_code`)
) ENGINE=InnoDB AUTO_INCREMENT=469 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='구군정보테이블';

DROP TABLE IF EXISTS `imageimporttask`;
CREATE TABLE `imageimporttask` (
  `task_key` binary(32) NOT NULL,
  `owner_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` int NOT NULL,
  `image_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_identity` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `object_key` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_hash` binary(32) DEFAULT NULL,
  `width` int DEFAULT NULL,
  `height` int DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `attempt_count` int NOT NULL DEFAULT '0',
  `reused_existing_object` tinyint(1) NOT NULL DEFAULT '0',
  `error_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_key`),
  KEY `idx_image_task_status` (`status`),
  KEY `idx_image_task_owner` (`owner_type`,`owner_id`),
  KEY `idx_image_task_content` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `sidos`;
CREATE TABLE `sidos` (
  `no` int NOT NULL AUTO_INCREMENT COMMENT '시도번호',
  `sido_code` int NOT NULL COMMENT '시도코드',
  `sido_name` varchar(20) DEFAULT NULL COMMENT '시도이름',
  PRIMARY KEY (`no`),
  UNIQUE KEY `sido_code_UNIQUE` (`sido_code`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='시도정보테이블';

-- Flyway migration changes are already incorporated into the lowercase table definitions above.
-- Crawl and drama match override tables are intentionally created last.
DROP TABLE IF EXISTS `crawlpost`;
CREATE TABLE `crawlpost` (
  `post_key` binary(32) NOT NULL,
  `post_url` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `post_title` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_at` datetime DEFAULT NULL,
  `drama_query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `normalized_query` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tmdb_id` int DEFAULT NULL,
  `content_hash` binary(32) DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_crawled_at` datetime DEFAULT NULL,
  PRIMARY KEY (`post_key`),
  UNIQUE KEY `uk_crawl_post_url` (`post_url`(768)),
  KEY `idx_crawl_post_status` (`status`),
  KEY `idx_crawl_post_tmdb` (`tmdb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `crawlscene`;
CREATE TABLE `crawlscene` (
  `source_key` binary(32) NOT NULL,
  `post_key` binary(32) NOT NULL,
  `source_order` int NOT NULL,
  `same_name_occurrence` int NOT NULL,
  `raw_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `raw_text` mediumtext COLLATE utf8mb4_unicode_ci,
  `raw_address` mediumtext COLLATE utf8mb4_unicode_ci,
  `image_urls` json DEFAULT NULL,
  `scene_id` int DEFAULT NULL,
  `status` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`source_key`),
  UNIQUE KEY `uk_crawl_scene_post_order` (`post_key`,`source_order`),
  KEY `idx_crawl_scene_status` (`status`),
  KEY `idx_crawl_scene_scene` (`scene_id`),
  CONSTRAINT `fk_crawl_scene_post` FOREIGN KEY (`post_key`) REFERENCES `crawlpost` (`post_key`),
  CONSTRAINT `fk_crawl_scene_scene` FOREIGN KEY (`scene_id`) REFERENCES `scene` (`scene_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `dramamatchoverride`;
CREATE TABLE `dramamatchoverride` (
  `normalized_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tmdb_id` int NOT NULL,
  `memo` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`normalized_title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Legacy Spring Batch metadata tables from DBSchema.sql.sql.
DROP TABLE IF EXISTS `batch_job_execution`;
CREATE TABLE `batch_job_execution` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_CODE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  KEY `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `batch_job_instance` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_job_execution_context`;
CREATE TABLE `batch_job_execution_context` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_job_execution_params`;
CREATE TABLE `batch_job_execution_params` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `PARAMETER_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PARAMETER_TYPE` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `PARAMETER_VALUE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `IDENTIFYING` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  KEY `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_job_execution_seq`;
CREATE TABLE `batch_job_execution_seq` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_job_instance`;
CREATE TABLE `batch_job_instance` (
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_KEY` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`),
  UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`,`JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_job_seq`;
CREATE TABLE `batch_job_seq` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_step_execution`;
CREATE TABLE `batch_step_execution` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint NOT NULL,
  `STEP_NAME` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `COMMIT_COUNT` bigint DEFAULT NULL,
  `READ_COUNT` bigint DEFAULT NULL,
  `FILTER_COUNT` bigint DEFAULT NULL,
  `WRITE_COUNT` bigint DEFAULT NULL,
  `READ_SKIP_COUNT` bigint DEFAULT NULL,
  `WRITE_SKIP_COUNT` bigint DEFAULT NULL,
  `PROCESS_SKIP_COUNT` bigint DEFAULT NULL,
  `ROLLBACK_COUNT` bigint DEFAULT NULL,
  `EXIT_CODE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  KEY `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `batch_job_execution` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_step_execution_context`;
CREATE TABLE `batch_step_execution_context` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `SERIALIZED_CONTEXT` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `batch_step_execution` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `batch_step_execution_seq`;
CREATE TABLE `batch_step_execution_seq` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Spring Batch metadata tables are intentionally created last.
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

-- Initial sequence rows required by Spring Batch.
INSERT IGNORE INTO `BATCH_STEP_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');
INSERT IGNORE INTO `BATCH_JOB_EXECUTION_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');
INSERT IGNORE INTO `BATCH_JOB_SEQ` (`ID`, `UNIQUE_KEY`) VALUES (0, '0');

SET FOREIGN_KEY_CHECKS = 1;
