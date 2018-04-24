/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.rbmhtechnology.vind.monitoring.report.Report;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created on 01.03.18.
 */
public class JsonReportWriter implements ReportWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonReportWriter.class);
    private ObjectMapper mapper;

    public JsonReportWriter() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    @Override
    public String write(Report report) {
        try {
            return mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            log.error("Error writing report as Json string: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean write(Report report, String reportFile) {
        try {
            final File file = new File(reportFile);
            mapper.writeValue(file, report);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Error writing report as Json string: {}", e.getMessage(), e);
            return false;
        } catch (IOException e) {
            log.error("Error writing report to file '{}': {}", reportFile, e.getMessage(), e);
            return false;
        }
    }
}
