/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.api.query.facet;

/**
 * Created on 05.11.18.
 */
public abstract class TermFacetOption {

    public abstract String getOption();

    public static class TermFacetPrefix extends TermFacetOption {

        private final String prefix;

        public TermFacetPrefix( String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        @Override
        public String getOption() {
            return "prefix";
        }
    }
}
