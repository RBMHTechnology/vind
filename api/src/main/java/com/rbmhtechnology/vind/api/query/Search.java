package com.rbmhtechnology.vind.api.query;

import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;

/**
 * Abstract class with static user friendly methods to instantiate different queries.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 28.06.16.
 */
public abstract class Search {

    /**
     * Instantiates a new {@link Update} query object for a document.
     * @param id String identifier of the document to update.
     * @return {@link Update} configuration.
     */
    public static Update update(String id) {
        return new Update(id);
    }

    /**
     * Instantiates a new {@link FulltextSearch} query object.
     * @return {@link FulltextSearch} configuration.
     */
    public static FulltextSearch fulltext() {
        return new FulltextSearch();
    }
    /**
     * Instantiates a new {@link FulltextSearch} query object.
     * @param text String text to search for.
     * @return {@link FulltextSearch} configuration.
     */
    public static FulltextSearch fulltext(String text) {
        return fulltext().text(text);
    }
    /**
     * Instantiates a new {@link SuggestionSearch} query object.
     * @return {@link SuggestionSearch} configuration.
     */
    public static SuggestionSearch suggest() {
        return new SuggestionSearch();
    }
    /**
     * Instantiates a new {@link SuggestionSearch} query object.
     * @param text String text to get suggestions for.
     * @return {@link SuggestionSearch} configuration.
     */
    public static SuggestionSearch suggest(String text) {
        return suggest().text(text);
    }

    /**
     * Instantiates a new {@link RealTimeGet} query object.
     * @param id String document id to retrieve.
     * @return {@link RealTimeGet} configuration.
     */
    public static RealTimeGet getById(String ... id) {
        return new RealTimeGet().get(id);
    }

}
