package com.rbmhtechnology.vind.test;

import org.junit.Rule;
import org.junit.Test;

import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static com.rbmhtechnology.vind.test.Backend.Solr;

public class ConditionalTest {


    @Rule
    public TestBackend testBackend = new TestBackend();

    @Test
    @RunWithBackend(Elastic)
    public void testA() {
        testBackend.getSearchServer();
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
