/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.analysis.report.writer;

import com.rbmhtechnology.vind.report.analysis.report.Report;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created on 01.03.18.
 */
public class JsonReportWriterTest {

    private JsonReportWriter reportWriter =  new JsonReportWriter();
    private Report report;

    @Before
    public void setUp() {

        final LinkedHashMap<String, List<Object>> fieldValuesMap = new LinkedHashMap<>();
        fieldValuesMap.put("field1", Arrays.asList("value1","value2","value3"));
        fieldValuesMap.put("field2", Arrays.asList("value4",5.00,"value6"));
        fieldValuesMap.put("field3", Arrays.asList("value7","value8",9));

        this.report = new Report()
                .setFrom(ZonedDateTime.now().minusDays(1))
                .setTo(ZonedDateTime.now().plusDays(1))
                .setRequests(10000)
                .setSuggestionFieldsValues(fieldValuesMap);
    }

    @Test
    public void testWriteToString() {
        final String jsonReport = reportWriter.write(report);

        //TODO add proper testing.
        Assert.assertTrue(true);

    }
}
