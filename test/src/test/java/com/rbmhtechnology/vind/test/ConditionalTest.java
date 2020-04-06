package com.rbmhtechnology.vind.test;

import org.junit.Rule;
import org.junit.Test;

import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static com.rbmhtechnology.vind.test.Backend.Solr;

public class ConditionalTest {

    @Rule
    public TestSearchServer testSearchServer = TestSearchServer.create();

    @Rule
    public RunsWithBackendRule runsWithBackendRule = new RunsWithBackendRule();

    @Test
    @RunWithBackend(Elastic)
    public void testA() {
        testSearchServer.getSearchServer();
        System.out.println("A");
    }

    @Test
    @RunWithBackend(Solr)
    public void testB() {
        System.out.println("B");
    }

    @Test
    @RunWithBackend({Solr, Elastic})
    public void testC() {
        System.out.println("C");
    }

    @Test
    public void testD() {
        System.out.println("C");
    }


}
