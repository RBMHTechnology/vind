package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.SearchServer;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class TestBackend extends ExternalResource implements TestRule {

    private static final Logger LOGGER = LoggerFactory.getLogger( TestBackend.class );

    private TestSearchServer testSearchServer;

    @Override
    public Statement apply(Statement base, Description description) {
        return new IgnorableStatement(base, description);
    }

    private static class IgnorableStatement extends Statement {

        private final Statement base;
        private final Description description;

        public IgnorableStatement(Statement base, Description description) {
            this.base = base;
            this.description = description;
        }

        @Override
        public void evaluate() throws Throwable {
            boolean shouldIgnore = false;
            RunWithBackend annotation = description.getAnnotation(RunWithBackend.class);
            if (annotation != null) {
                shouldIgnore = Arrays.stream(annotation.value()).noneMatch((Backend::isActive));
            }
            Assume.assumeTrue("Test is ignored!", !shouldIgnore);
            base.evaluate();
        }
    }

    public SearchServer getSearchServer() {
        if(testSearchServer == null) {
            switch (ServerType.current()) {
                case Elastic:
                    testSearchServer = new ElasticTestSearchServer();
                    break;
                default:
                    testSearchServer = new TestSearchServer();
            }
        }
        testSearchServer.start();
        return testSearchServer.getSearchServer();
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }

    @Override
    protected void after() {
        try {
            if(testSearchServer != null) {
                testSearchServer.close();
            }
        } catch (SearchServerException e) {
            LOGGER.error("Error closing SearchServer: {}", e.getMessage(), e);
        } finally {
            System.getProperties().remove("runtimeLib");
            super.after();
        }

    }
}
