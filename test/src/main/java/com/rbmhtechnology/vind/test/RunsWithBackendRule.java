package com.rbmhtechnology.vind.test;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Arrays;

public class RunsWithBackendRule implements TestRule {
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
}
