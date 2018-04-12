/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.elastic.writer;

import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;
import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 01.03.18.
 */
public class ElasticWriter extends MonitoringWriter {

    private static final Logger logger = LoggerFactory.getLogger(ElasticWriter.class);
    private ElasticSearchClient elasticClient;

    public ElasticWriter(String elasticHost, String elasticPort, String elasticIndex) {
        logger.debug("ElasticWriter configured to write in: {}:{}/{}", elasticHost, elasticPort, elasticIndex);
        elasticClient = new ElasticSearchClient();
        elasticClient.init(elasticHost, elasticPort, elasticIndex);
    }

    @Override
    public void log(MonitoringEntry log) {
        logger.debug("Indexing log entry: {}", log.toJson());
        elasticClient.put(log.toJson());
    }
}
