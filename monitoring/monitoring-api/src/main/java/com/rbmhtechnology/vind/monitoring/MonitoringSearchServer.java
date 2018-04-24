package com.rbmhtechnology.vind.monitoring;

import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.*;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;
import com.rbmhtechnology.vind.monitoring.logger.entry.*;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.application.SimpleApplication;
import com.rbmhtechnology.vind.monitoring.model.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class MonitoringSearchServer extends SearchServer {

    private Logger log = LoggerFactory.getLogger(MonitoringSearchServer.class);

    public static final String APPLICATION_ID = "search.monitoring.application.id";


    private final SearchServer server;

    private Session session;
    private Application application;
    private HashMap<String, Object> monitoringMetadata = new HashMap<>();

    private boolean silent = false;

    private final MonitoringWriter logger;

    public MonitoringSearchServer(SearchServer server) {
        this(server, null, null, MonitoringWriter.getInstance()); //TODO should maybe replaced by service loader?
    }

    public MonitoringSearchServer(SearchServer server, MonitoringWriter logger) {
        this(server, null, null, logger); //TODO should maybe replaced by service loader?
    }

    public MonitoringSearchServer(SearchServer server, Application application) {
        this(server, application, null, MonitoringWriter.getInstance()); //TODO should maybe replaced by service loader?
    }

    public MonitoringSearchServer(SearchServer server, Application application, MonitoringWriter logger) {
        this(server, application, null, logger); //TODO should maybe replaced by service loader?
    }

    public MonitoringSearchServer(SearchServer server, Application application, Session session) {
        this(server, application, session, MonitoringWriter.getInstance()); //TODO should maybe replaced by service loader?
    }

    public MonitoringSearchServer(SearchServer server, Application application, Session session, MonitoringWriter logger) {
        this.server = server;
        this.session = session;
        this.logger = logger;

        if(application == null) {
            String applicationId = SearchConfiguration.get(APPLICATION_ID);

            if(applicationId == null) {
                log.error("property '{}' has to be set for search monitoring", APPLICATION_ID);
                throw new RuntimeException("Property '" + APPLICATION_ID + "' has to be set for search monitoring");
            }

            application = new SimpleApplication(applicationId);
        }
        this.application = application;
    }

    @Override
    public Object getBackend() {
        return server.getBackend();
    }

    @Override
    public IndexResult index(Document... docs) {
       return this.index(Arrays.asList(docs));
    }

    @Override
    public IndexResult index(List<Document> docs) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is indexing document'{}' at {}:{}:{} - {}.{}.{} ", docs.stream().map(Document::getId).collect(Collectors.toList()),
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final IndexResult result =  server.index(docs);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final IndexEntry entry =
                    new IndexEntry( application, start, end, result.getQueryTime(), result.getElapsedTime(), session, docs);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding an Index entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Index monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }

        return server.index(docs);
    }

    @Override
    public boolean execute(Update update, DocumentFactory factory) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is updating document'{}' at {}:{}:{} - {}.{}.{} ", update.getId(),
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final Boolean result =  server.execute(update, factory);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final UpdateEntry entry =
                    new UpdateEntry( application, start, end, session, update,result);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding an Update entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Update monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public DeleteResult execute(Delete delete, DocumentFactory factory) {

        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is deleting at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final DeleteResult result = server.execute(delete, factory);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final DeleteEntry entry =
                    new DeleteEntry(application, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Delete entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Delete monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public DeleteResult delete(Document doc) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is deleting at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final DeleteResult result = server.delete(doc);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final DeleteEntry entry =
                    new DeleteEntry(application, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Delete entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Delete monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public void commit(boolean optimize) {
        //currently not logged
        server.commit(optimize);
    }

    @Override
    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing FulltextSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final BeanSearchResult<T> result = server.execute(search, c);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final FullTextEntry entry =
                    new FullTextEntry(this.server, AnnotationUtil.createDocumentFactory(c), application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a FulltextSearch entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Fulltext monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c, HashMap<String, Object> metadata) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing FulltextSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final BeanSearchResult<T> result = server.execute(search, c);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final FullTextEntry entry =
                    new FullTextEntry(this.server, AnnotationUtil.createDocumentFactory(c), application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);

            final HashMap<String, Object> mergedMetadata = new HashMap<>();
            mergedMetadata.putAll(this.monitoringMetadata);
            mergedMetadata.putAll(metadata);
            entry.setMetadata(mergedMetadata);
            log.debug("Monitoring is adding a FulltextSearch entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Fulltext monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public SearchResult execute(FulltextSearch search, DocumentFactory factory) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing FulltextSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SearchResult result = server.execute(search, factory);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final FullTextEntry entry = new FullTextEntry(this.server, factory, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a FulltextSearch entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Fulltext monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    public SearchResult execute(FulltextSearch search, DocumentFactory factory, HashMap<String, Object> metadata) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing FulltextSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SearchResult result = server.execute(search, factory);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final FullTextEntry entry = new FullTextEntry(this.server, factory, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);

            final HashMap<String, Object> mergedMetadata = new HashMap<>();
            mergedMetadata.putAll(this.monitoringMetadata);
            mergedMetadata.putAll(metadata);
            entry.setMetadata(mergedMetadata);
            log.debug("Monitoring is adding a FulltextSearch entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Fulltext monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public String getRawQuery(FulltextSearch search, DocumentFactory factory) {
        return server.getRawQuery(search,factory);
    }

    @Override
    public <T> String getRawQuery(FulltextSearch search, Class<T> c) {
        return server.getRawQuery(search,c);
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, c);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, AnnotationUtil.createDocumentFactory(c), application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c, HashMap<String, Object> metadata) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, c);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, AnnotationUtil.createDocumentFactory(c), application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            // Adding execution metadata to server metadata
            final HashMap<String, Object> mergedMetadata = new HashMap<>();
            mergedMetadata.putAll(this.monitoringMetadata);
            mergedMetadata.putAll(metadata);
            entry.setMetadata(mergedMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, assets);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, assets, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, HashMap<String, Object> metadata) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, assets);
        final ZonedDateTime end = ZonedDateTime.now();
        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, assets, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            // Adding execution metadata to server metadata
            final HashMap<String, Object> mergedMetadata = new HashMap<>();
            mergedMetadata.putAll(this.monitoringMetadata);
            mergedMetadata.putAll(metadata);
            entry.setMetadata(mergedMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, assets, childFactory);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, assets, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory, HashMap<String, Object> metadata) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing SuggestionSearch at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final SuggestionResult result = server.execute(search, assets, childFactory);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final SuggestionEntry entry = new SuggestionEntry(this.server, assets, application, search, result, start, end, result.getQueryTime(), result.getElapsedTime(), session);
            // Adding execution metadata to server metadata
            final HashMap<String, Object> mergedMetadata = new HashMap<>();
            mergedMetadata.putAll(this.monitoringMetadata);
            mergedMetadata.putAll(metadata);
            entry.setMetadata(mergedMetadata);
            log.debug("Monitoring is adding a Suggestion entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Suggestion monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {
        return server.getRawQuery(search,factory);
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory) {
        return server.getRawQuery(search,factory, childFactory);
    }

    @Override
    public <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c) {
        return server.getRawQuery(search, c);
    }


    @Override
    public <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing Real time get at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final BeanGetResult<T> result = server.execute(search, c);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final GetEntry entry = new GetEntry(application, start, end,result.getQueryTime(), result.getElapsedTime(), session, search.getValues(), result.getNumOfResults());
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Get entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Get monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        final ZonedDateTime start = ZonedDateTime.now();
        log.debug("Monitoring server is executing Real time get at {}:{}:{} - {}.{}.{} ",
                start.getHour(),start.getMinute(),start.getSecond(),start.getDayOfMonth(),start.getMonth(),start.getYear());
        final GetResult result = server.execute(search, assets);
        final ZonedDateTime end = ZonedDateTime.now();

        try {
            final GetEntry entry = new GetEntry(application, start, end,result.getQueryTime(), result.getElapsedTime(), session, search.getValues(), result.getNumOfResults());
            entry.setMetadata(this.monitoringMetadata);
            log.debug("Monitoring is adding a Get entry");
            logger.log(entry);
        } catch (Exception e) {
            log.error("Get monitoring error: {}", e.getMessage(), e);
            if (!silent) {
                throw e;
            }
        }
        return result;
    }

    @Override
    public void clearIndex() {
        server.clearIndex();
    }

    @Override
    public void close() {
        server.close();
    }

    @Override
    public Class<ServiceProvider> getServiceProviderClass() {
        return this.server.getServiceProviderClass();
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void addMetadata(String key, Object value) {
        if (Objects.nonNull(key)) {
            this. monitoringMetadata.put(key, value);
        }
    }

    public boolean isSilent() {
        return silent;
    }

    public MonitoringSearchServer setSilent(boolean silent) {
        this.silent = silent;
        return this;
    }
}
