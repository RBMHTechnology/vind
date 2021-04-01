package com.rbmhtechnology.vind.api.query.suggestion;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 07.07.16.
 */
public interface ExecutableSuggestionSearch {
    public boolean isStringSuggestion();

    public boolean hasFilter();

    public Filter getFilter();

    public String getInput();

    public Sort getSort();

    public ExecutableSuggestionSearch text(String text);

    public ExecutableSuggestionSearch filter(Filter filter);

    public ExecutableSuggestionSearch setLimit(int limit);

    public ExecutableSuggestionSearch context(String context);

    public int getLimit();

    String getSearchContext();
}
