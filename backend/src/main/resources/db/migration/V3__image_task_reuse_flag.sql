DELIMITER $$

DROP PROCEDURE IF EXISTS pilgrimage_add_image_reuse_flag $$
CREATE PROCEDURE pilgrimage_add_image_reuse_flag()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ImageImportTask'
      AND COLUMN_NAME = 'reused_existing_object'
  ) THEN
    ALTER TABLE `ImageImportTask`
      ADD COLUMN `reused_existing_object` BOOLEAN NOT NULL DEFAULT FALSE AFTER `attempt_count`;
  END IF;
END $$

DELIMITER ;

CALL pilgrimage_add_image_reuse_flag();

DROP PROCEDURE IF EXISTS pilgrimage_add_image_reuse_flag;
