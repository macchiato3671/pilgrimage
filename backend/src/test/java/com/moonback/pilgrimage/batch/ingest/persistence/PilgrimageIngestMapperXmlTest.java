package com.moonback.pilgrimage.batch.ingest.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class PilgrimageIngestMapperXmlTest {

	@Test
	void mapperXmlLoadsWithoutAnnotationSql() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setMapUnderscoreToCamelCase(true);
		try (InputStream input = Resources.getResourceAsStream("mapper/PilgrimageIngestMapper.xml")) {
			XMLMapperBuilder parser = new XMLMapperBuilder(input, configuration,
					"mapper/PilgrimageIngestMapper.xml", configuration.getSqlFragments());
			parser.parse();
		}

		assertThat(configuration.hasStatement(
				"com.moonback.pilgrimage.batch.ingest.persistence.PilgrimageIngestMapper.countDrama")).isTrue();
		assertThat(configuration.hasStatement(
				"com.moonback.pilgrimage.batch.ingest.persistence.PilgrimageIngestMapper.upsertDrama")).isTrue();
	}
}
