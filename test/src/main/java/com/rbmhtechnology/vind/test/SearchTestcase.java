package com.rbmhtechnology.vind.test;

import org.junit.Rule;

/**
 * TODO does not seem to work, as the rule is evaluated before the instance is there
 * Provides some helper methods
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 21.06.16.
 */
public class SearchTestcase {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

}
