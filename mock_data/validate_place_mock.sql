-- Place Mock Data validation template.
-- For seed 3671 with the default config: slot = 3671 % 64 = 23.
-- Change these values when --seed or content_id namespace settings change.
SET @mock_content_id_start = 1838000000;
SET @mock_content_id_end = 1843999999;

SELECT 'total' AS metric, COUNT(*) AS value
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end;

SELECT content_type_id, COUNT(*) AS row_count
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end
GROUP BY content_type_id
ORDER BY content_type_id;

SELECT
  CASE
    WHEN address LIKE '서울특별시 %'
      OR address LIKE '인천광역시 %'
      OR address LIKE '경기도 %' THEN '수도권'
    WHEN address LIKE '부산광역시 %'
      OR address LIKE '울산광역시 %' THEN '부산/울산'
    WHEN address LIKE '대구광역시 %'
      OR address LIKE '경상북도 %' THEN '대구/경북'
    WHEN address LIKE '대전광역시 %'
      OR address LIKE '세종특별자치시 %'
      OR address LIKE '충청북도 %'
      OR address LIKE '충청남도 %' THEN '대전/충청'
    WHEN address LIKE '광주광역시 %'
      OR address LIKE '전라북도 %'
      OR address LIKE '전라남도 %' THEN '광주/전라'
    WHEN address LIKE '강원특별자치도 %' THEN '강원'
    WHEN address LIKE '제주특별자치도 %' THEN '제주'
    ELSE '기타'
  END AS region,
  COUNT(*) AS row_count
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end
GROUP BY region
ORDER BY region;

SELECT
  SUM(description IS NULL) AS description_null,
  SUM(description IS NOT NULL AND CHAR_LENGTH(description) < 30) AS description_short,
  SUM(description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 30 AND 100) AS description_30_100,
  SUM(description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 101 AND 300) AS description_101_300,
  SUM(description IS NOT NULL AND CHAR_LENGTH(description) BETWEEN 301 AND 700) AS description_301_700,
  SUM(description IS NOT NULL AND CHAR_LENGTH(description) >= 701) AS description_701_plus
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end;

SELECT
  MIN(latitude) AS min_latitude,
  MAX(latitude) AS max_latitude,
  MIN(longitude) AS min_longitude,
  MAX(longitude) AS max_longitude,
  SUM(
    latitude < 33.0 OR latitude > 38.6
    OR longitude < 126.0 OR longitude > 129.7
  ) AS outside_korea_bounds
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end;

SELECT
  COUNT(*) - COUNT(DISTINCT content_id) AS duplicate_content_id,
  SUM(src_created_at > src_updated_at) AS invalid_date_order,
  MIN(src_created_at) AS min_src_created_at,
  MAX(src_updated_at) AS max_src_updated_at
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end;

SELECT
  SUM(name LIKE '%공원%') AS common_park,
  SUM(name LIKE '%문화%') AS medium_culture,
  SUM(name LIKE '%별빛누리%') AS rare_star_token
FROM place
WHERE content_id BETWEEN @mock_content_id_start AND @mock_content_id_end;
