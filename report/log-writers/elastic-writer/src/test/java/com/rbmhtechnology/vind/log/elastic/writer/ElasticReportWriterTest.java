/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.log.elastic.writer;

import com.rbmhtechnology.vind.report.logger.entry.FullTextEntry;
import org.junit.Test;

/**
 * Created on 01.03.18.
 */
public class ElasticReportWriterTest {

    @Test
    public void logTest(){
        final ElasticReportWriter elasticWriter =
                new ElasticReportWriter("localhost", "9201", "logindex");

        elasticWriter.log(new FullTextEntry());
    }
}
