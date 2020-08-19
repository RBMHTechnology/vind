package com.rbmhtechnology.vind.api;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.BeanGetResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.DeleteResult;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.InverseSearchResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.StatusResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.InverseSearchQuery;

import java.util.Collections;
import java.util.List;

public class MasterSlaveSearchServer extends SearchServer {

    private final SearchServer backend;
    private final CompletableSearchServer slaveBackend;

    public MasterSlaveSearchServer(SearchServer backend, SearchServer slaveBackend) {
        this.backend = backend;
        this.slaveBackend = new CompletableSearchServer(slaveBackend);
    }

    public Object getSlaveBackend() {
        return this.slaveBackend.getBackend();
    }

    @Override
    public Object getBackend() {
        return this.backend;
    }

    @Override
    public StatusResult getBackendStatus() {
        final StatusResult masterStatus = this.backend.getBackendStatus();
        final StatusResult slaveStatus = this.slaveBackend.getBackendStatus();
        if (masterStatus.getStatus().equals(StatusResult.Status.UP) &&
                slaveStatus.getStatus().equals(StatusResult.Status.UP)) {
            return StatusResult.up()
                    .setDetail("master", masterStatus)
                    .setDetail("slave", slaveStatus);
        }
        return StatusResult.down()
                .setDetail("master", masterStatus)
                .setDetail("slave", slaveStatus);
    }

    @Override
    public IndexResult index(Document... doc) {
        slaveBackend.indexAsync(doc);
        return backend.index(doc);
    }

    @Override
    public IndexResult index(List<Document> doc) {
        slaveBackend.indexAsync(doc);
        return backend.index(doc);
    }

    @Override
    public IndexResult indexWithin(Document doc, int withinMs) {
        slaveBackend.indexAsyncWithin(Collections.singletonList(doc), withinMs);
        return backend.indexWithin(doc, withinMs);
    }

    @Override
    public IndexResult indexWithin(List<Document> docs, int withinMs) {
        slaveBackend.indexAsyncWithin(docs, withinMs);
        return backend.indexWithin(docs, withinMs);
    }

    @Override
    public DeleteResult delete(Document doc) {
        slaveBackend.deleteAsync(doc);
        return backend.delete(doc);
    }

    @Override
    public DeleteResult deleteWithin(Document doc, int withinMs) {
        slaveBackend.deleteAsyncWithin(doc, withinMs);
        return backend.deleteWithin(doc, withinMs);
    }

    @Override
    public boolean execute(Update update, DocumentFactory factory) {
        slaveBackend.executeAsync(update, factory);
        return backend.execute(update, factory);
    }

    @Override
    public DeleteResult execute(Delete delete, DocumentFactory factory) {
        slaveBackend.executeAsync(delete, factory);
        return backend.execute(delete, factory);
    }

    @Override
    public void commit(boolean optimize) {
        slaveBackend.commitAsync();
        backend.commit();
    }

    @Override
    protected <T> BeanSearchResult<T> executeInternal(FulltextSearch search, Class<T> c) {
        return backend.execute(search, c);
    }

    @Override
    protected SearchResult executeInternal(FulltextSearch search, DocumentFactory factory) {
        return backend.execute(search, factory);
    }

    @Override
    public String getRawQuery(FulltextSearch search, DocumentFactory factory) {
        return backend.getRawQuery(search, factory);
    }

    @Override
    public <T> String getRawQuery(FulltextSearch search, Class<T> c) {
        return backend.getRawQuery(search, c);
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        return backend.execute(search, c);
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets) {
        return backend.execute(search, assets);
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        return backend.execute(search, assets, childFactory);
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {
        return backend.getRawQuery(search, factory);
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory) {
        return backend.getRawQuery(search, factory, childFactory);
    }

    @Override
    public <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c) {
        return backend.getRawQuery(search, c);
    }

    @Override
    public <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c) {
        return backend.execute(search, c);
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        return backend.execute(search, assets);
    }

    @Override
    public InverseSearchResult execute(InverseSearch inverseSearch, DocumentFactory factory) {
        return backend.execute(inverseSearch, factory);
    }

    @Override
    public IndexResult addInverseSearchQuery(InverseSearchQuery query) {
        return backend.addInverseSearchQuery(query);
    }


    @Override
    public void clearIndex() {
        slaveBackend.clearIndex();
        backend.clearIndex();
    }

    @Override
    public void close() {
        slaveBackend.close();
        backend.close();
    }

    @Override
    public Class<? extends ServiceProvider> getServiceProviderClass() {
        return backend.getServiceProviderClass();
    }

    @Override
    protected FulltextSearch smartParse(FulltextSearch search, DocumentFactory factory) {
        return search;
    }

    @Override
    protected <T> FulltextSearch smartParse(FulltextSearch search, Class<T> c) {
        return search;
    }
}
