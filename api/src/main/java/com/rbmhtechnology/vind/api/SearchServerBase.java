package com.rbmhtechnology.vind.api;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.parser.queryparser.VindQueryParser;

public abstract class SearchServerBase extends SearchServer {

    @Override
    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c) {
        if(search.isSmartParsing()) {
            return doExecute(smartParse(search, c),c);
        } else {
            return doExecute(search, c);
        }
    }
    protected abstract <T> BeanSearchResult<T> doExecute(FulltextSearch search, Class<T> c);

    @Override
    public SearchResult execute(FulltextSearch search, DocumentFactory factory) {
        if(search.isSmartParsing()) {
            return doExecute(smartParse(search, factory),factory);
        } else {
            return doExecute(search, factory);
        }
    }

    protected abstract SearchResult doExecute(FulltextSearch search, DocumentFactory factory);

    protected FulltextSearch smartParse(FulltextSearch search, DocumentFactory factory) {
        final VindQueryParser parser = new VindQueryParser();

        final FulltextSearch smartSearch = parser.parse(search.getSearchString(), factory);
        search.text(smartSearch.getSearchString());
        search.filter(smartSearch.getFilter());
        return search;
    }

    protected <T> FulltextSearch smartParse(FulltextSearch search, Class<T> c) {
        final DocumentFactory factory = AnnotationUtil.createDocumentFactory(c);
        return smartParse(search, factory);
    }

}
