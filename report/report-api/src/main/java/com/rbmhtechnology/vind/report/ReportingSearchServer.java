package com.rbmhtechnology.vind.report;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.report.application.Application;
import com.rbmhtechnology.vind.report.application.SimpleApplication;
import com.rbmhtechnology.vind.report.logger.Log;
import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class ReportingSearchServer extends SearchServer {

    private Logger log = LoggerFactory.getLogger(ReportingSearchServer.class);

    public static final String APPLICATION_ID = "reporting.application.id";


    private final SearchServer server;

    private Session session;

    private Application application;

    private final ReportWriter logger;

    public ReportingSearchServer(SearchServer server) {
        this(server, null, null, ReportWriter.getInstance()); //TODO should maybe replaced by service loader?
    }

    public ReportingSearchServer(SearchServer server, Application application, Session session) {
        this(server, application, session, ReportWriter.getInstance()); //TODO should maybe replaced by service loader?
    }

    public ReportingSearchServer(SearchServer server, Application application, Session session, ReportWriter logger) {
        this.server = server;
        this.session = session;
        this.logger = logger;

        if(application == null) {
            String applicationId = SearchConfiguration.get(APPLICATION_ID);

            if(applicationId == null) {
                log.error("property '{}' has to be set for report logger", APPLICATION_ID);
                throw new RuntimeException("property '" + APPLICATION_ID + "' has to be set for report logger");
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
    public void index(Document... docs) {
        server.index(docs);
    }

    @Override
    public void index(List<Document> docs) {
        server.index(docs);
    }

    @Override
    public void execute(Update update, DocumentFactory factory) {
        //currently not logged
        server.execute(update, factory);
    }

    @Override
    public void execute(Delete delete, DocumentFactory factory) {
        server.execute(delete,factory);
    }

    @Override
    public void delete(Document doc) {
        //currently not logged
        server.delete(doc);
    }

    @Override
    public void commit(boolean optimize) {
        //currently not logged
        server.commit(optimize);
    }

    @Override
    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c) {
        BeanSearchResult<T> result = server.execute(search, c);
        //TODO log
        return result;
    }

    @Override
    public SearchResult execute(FulltextSearch search, DocumentFactory factory) {
        ZonedDateTime start = ZonedDateTime.now();
        SearchResult result = server.execute(search, factory);
        //TODO log: this is just a simple test
        logger.log(new Log(application, search, factory.getType(), result, start, session));
        return result;
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        SuggestionResult result = server.execute(search, c);
        //TODO log
        return result;
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets) {
        SuggestionResult result = server.execute(search, assets);
        //TODO log
        return result;
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        SuggestionResult result = server.execute(search, assets, childFactory);
        //TODO log
        return result;
    }

    @Override
    public <T> GetResult execute(RealTimeGet search, Class<T> c) {
        final GetResult result = server.execute(search, c);
        //TODO log
        return result;
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        final GetResult result = server.execute(search, assets);
        //TODO log
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
}
