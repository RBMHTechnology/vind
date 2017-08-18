package com.rbmhtechnology.vind.other;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fonso on 12/1/16.
 */
public class OtherTestSearchServer extends SearchServer {

    private static final Logger log = LoggerFactory.getLogger(OtherTestSearchServer.class);
    private static final Logger solrClientLogger = LoggerFactory.getLogger(log.getName() + "#solrClient");

    public OtherTestSearchServer() {
        log.warn("Creating a fake server");
    }

    @Override
    public Object getBackend() {
        return null;
    }

    @Override
    public void index(Document... doc) {

    }

    @Override
    public void index(List<Document> doc) {

    }

    @Override
    public void delete(Document doc) {

    }

    @Override
    public void execute(Update update, DocumentFactory factory) {

    }

    @Override
    public void execute(Delete delete, DocumentFactory factory) {

    }

    @Override
    public void commit(boolean optimize) {

    }

    @Override
    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c) {
        return null;
    }

    @Override
    public SearchResult execute(FulltextSearch search, DocumentFactory factory) {
        return null;
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        return null;
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets) {
        return null;
    }

    @Override
    public <T> GetResult execute(RealTimeGet search, Class<T> c) {
        return null;
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        return null;
    }

    @Override
    public void clearIndex() {

    }

    @Override
    public void close() {

    }

    @Override
    public Class getServiceProviderClass() {
        return new OtherTestServerProvider().getClass() ;
    }
}
