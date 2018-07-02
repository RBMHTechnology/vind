/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.writer;

import com.rbmhtechnology.vind.monitoring.report.Report;
import com.rbmhtechnology.vind.monitoring.report.configuration.ReportConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;

/**
 * Created on 01.03.18.
 */
public class JsonReportWriterTest {

    private JsonReportWriter reportWriter =  new JsonReportWriter();
    private Report report;

    @Before
    public void setUp() {

        final LinkedHashMap<Object, Long> fieldsValues = new LinkedHashMap<>();
        fieldsValues.put("value1", 8l);
        fieldsValues.put("value2", 3l);
        fieldsValues.put("value3", 1l);

        final LinkedHashMap<String, LinkedHashMap<Object, Long>> suggestionFieldsValues = new LinkedHashMap<>();
        suggestionFieldsValues.put("field1", fieldsValues);

        ReportConfiguration config = new ReportConfiguration().setApplicationId("Test HTML report application");

        this.report = new Report(config)
                .setFrom(ZonedDateTime.now().minusDays(1))
                .setTo(ZonedDateTime.now().plusDays(1))
                .setRequests(10000)
                .setSuggestionFieldsValues(suggestionFieldsValues);
    }

    @Test
    public void testWriteToString() {
        final String jsonReport = reportWriter.write(report);

        //TODO add proper testing.
        Assert.assertTrue(true);

    }
}
