/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.writer;


import com.rbmhtechnology.vind.monitoring.report.Report;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created on 01.03.18.
 */
public class HtmlReportWriterTest {

    private HtmlReportWriter reportWriter =  new HtmlReportWriter();
    private Report report;

    @Before
    public void setUp() {

        final LinkedHashMap<String, Long> fieldsMap = new LinkedHashMap<>();
        fieldsMap.put("field1", 34000l);
        fieldsMap.put("field2", 10453l);
        fieldsMap.put("field3", 3957l);

        final LinkedHashMap<Object, Long> valuesMap = new LinkedHashMap<>();
        valuesMap.put("value1", 3400l);
        valuesMap.put(45.6, 104l);
        valuesMap.put("value3", 39l);

        final LinkedHashMap<String, LinkedHashMap<Object, Long>> fieldsValuesMap = new LinkedHashMap<>();
        fieldsValuesMap.put("field1", valuesMap);
        fieldsValuesMap.put("field2", valuesMap);
        fieldsValuesMap.put("field3", valuesMap);


        final LinkedHashMap<ZonedDateTime, Long> topDays = new LinkedHashMap<>();
        topDays.put(ZonedDateTime.now().minusDays(2), 23204l);
        topDays.put(ZonedDateTime.now().plusDays(5), 2271l);

        final LinkedHashMap<String, Long> topQueries = new LinkedHashMap<>();
        topQueries.put("*", 204l);
        topQueries.put("search 1", 22l);

        final LinkedHashMap<String, Long> topUsers = new LinkedHashMap<>();
        topUsers.put("User 1", 204l);
        topUsers.put("User 2", 22l);


        this.report = new Report()
                .setApplicationName("Test HTML report application")
                .setFrom(ZonedDateTime.now().minusDays(1))
                .setTo(ZonedDateTime.now().plusDays(1))
                .setRequests(10000)
                .setTopSuggestionFields(fieldsMap)
                .setFacetFieldsValues(fieldsValuesMap)
                .setTopFacetFields(fieldsMap)
                .setTopDays(topDays)
                .setTopQueries(topQueries)
                .setTopUsers(topUsers);


        final HashMap<String, String> fieldExtension = new HashMap<>();
        fieldExtension.put("field2", "1");
        fieldExtension.put("field3", "2");

        report.getConfiguration()
                .addGeneralFilter("Module","Assets")
                .addFacetFieldExtension("Position", fieldExtension)
                .addFacetFieldExtension("extra column", null)
        ;
    }

    @Test
    public void testWriteToString() {
        final String htmlReport = reportWriter.write(report);

        //TODO add proper testing.
        Assert.assertTrue(true);

    }
}
