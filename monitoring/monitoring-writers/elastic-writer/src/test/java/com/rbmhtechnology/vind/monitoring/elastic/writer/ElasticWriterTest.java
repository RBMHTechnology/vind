/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.elastic.writer;

import com.rbmhtechnology.vind.monitoring.logger.entry.FullTextEntry;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created on 01.03.18.
 */
public class ElasticWriterTest {

    @Test
    @Ignore
    public void logTest(){
        final ElasticWriter elasticWriter =
                new ElasticWriter("localhost", "9200", "logindex");

        elasticWriter.log(new FullTextEntry());
    }
}
