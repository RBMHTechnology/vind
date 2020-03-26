package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.BeanGetResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.DeleteResult;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ElasticSearchServer extends SearchServer {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);

    @Override
    public Object getBackend() {
        return null;
    }

    @Override
    public IndexResult index(Document... doc) {
        return null;
    }

    @Override
    public IndexResult index(List<Document> doc) {
        return null;
    }

    @Override
    public IndexResult indexWithin(Document doc, int withinMs) {
        return null;
    }

    @Override
    public IndexResult indexWithin(List<Document> doc, int withinMs) {
        return null;
    }

    @Override
    public DeleteResult delete(Document doc) {
        return null;
    }

    @Override
    public DeleteResult deleteWithin(Document doc, int withinMs) {
        return null;
    }

    @Override
    public boolean execute(Update update, DocumentFactory factory) {
        return false;
    }

    @Override
    public DeleteResult execute(Delete delete, DocumentFactory factory) {
        return null;
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
    public String getRawQuery(FulltextSearch search, DocumentFactory factory) {
        return null;
    }

    @Override
    public <T> String getRawQuery(FulltextSearch search, Class<T> c) {
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
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        return null;
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {
        return null;
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory) {
        return null;
    }

    @Override
    public <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c) {
        return null;
    }

    @Override
    public <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c) {
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
    public Class<ServiceProvider> getServiceProviderClass() {
        return null;
    }
}
