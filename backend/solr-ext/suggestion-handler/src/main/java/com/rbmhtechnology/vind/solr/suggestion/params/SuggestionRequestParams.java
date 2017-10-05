package com.rbmhtechnology.vind.solr.suggestion.params;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SuggestionRequestParams {

    public static final String SUGGESTION = "suggestion";
    public static final String SUGGESTION_FIELD = "suggestion.field";
    public static final String SUGGESTION_MULTIVALUE_FIELD = "suggestion.multivalue.field";
    public static final String SUGGESTION_LIMIT = "suggestion.limit";
    public static final String SUGGESTION_LIMIT_TYPE = "suggestion.limittype";
    public static final String SUGGESTION_DF = "suggestion.df";

    public static final String SUGGESTION_TERM_LIMIT = "suggestion.term.limit";
    public static final String SUGGESTION_INTERNAL_LIMIT = "suggestion.internal.limit";
    public static final String SUGGESTION_STRATEGY = "suggestion.strategy";

    public static final String SUGGESTION_INTERVAL = "suggestion.interval";
    public static final String SUGGESTION_INTERVAL_LABEL = "suggestion.interval.label";
    public static final String SUGGESTION_INTERVAL_OTHER = "suggestion.interval.other";
    public static final String SUGGESTION_INTERVAL_FIELD = "suggestion.interval.field";
    public static final String SUGGESTION_INTERVAL_RANGE_START = "suggestion.interval.range.%s.start";
    public static final String SUGGESTION_INTERVAL_RANGE_END = "suggestion.interval.range.%s.end";
    public static final String SUGGESTION_INTERVAL_RANGE_SCORE = "suggestion.interval.range.%s.score";
}
