/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.log.elastic.writer;

import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.logger.entry.LogEntry;
import com.rbmhtechnology.vind.report.utils.ElasticSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 01.03.18.
 */
public class ElasticReportWriter extends ReportWriter {

    private static final Logger logger = LoggerFactory.getLogger(ElasticReportWriter.class);
    private ElasticSearchClient elasticClient;

    public ElasticReportWriter(String elasticHost, String elasticPort, String elasticIndex) {
        elasticClient = new ElasticSearchClient();
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    @Override
    public void log(LogEntry log) {
        logger.debug("Indexing log entry: {}", log.toJson());
        elasticClient.put(log.toJson());
    }
}
