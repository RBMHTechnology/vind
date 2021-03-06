package com.rbmhtechnology.vind.api;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.SearchServerInstantiateException;
import com.rbmhtechnology.vind.SearchServerProviderLoaderException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
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
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Abstract class  which offers a common set of methods to be implemented by the specific server implementations
 * (Solr, ElasticSearch,..).
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Jakob Frank (jakob.frank@redlink.co)
 * @since 15.06.16.
 */
public abstract class SearchServer implements Closeable {

    protected static Logger log = LoggerFactory.getLogger(SearchServer.class);

    /**
     * Gets a {@link SearchServer} implementation object defined in the classpath from the ServiceLoader.
     * @return {@link SearchServer} specific implementation.
     */
    public static SearchServer getInstance() {

        String providerClassName = SearchConfiguration.get(SearchConfiguration.SERVER_PROVIDER, null);

        //Backwards compatibility needed
        final String solrProviderClassName = SearchConfiguration.get(SearchConfiguration.SERVER_SOLR_PROVIDER, null);
        if (providerClassName == null && solrProviderClassName != null) {
            providerClassName = solrProviderClassName;
        }

        final List<Exception> errorMessages = new ArrayList<>();
        final ServiceLoader<SearchServer> loader = ServiceLoader.load(SearchServer.class);
        final Iterator<SearchServer> it = loader.iterator();
        SearchServer server = null;

        if (!it.hasNext()) {
            log.error("No SearchServer in classpath");
            throw new RuntimeException("No SearchServer in classpath");
        } else {
            //if there is no service provider specified the first one found will work
            if (providerClassName == null) {
                server = it.next();
            } else {
                try {
                    final Class<?> providerClass = Class.forName(providerClassName);
                    while (it.hasNext() && server == null) {
                        try {
                            server = it.next();
                            if (server == null
                                    || server.getServiceProviderClass() == null
                                    || !server.getServiceProviderClass().getCanonicalName().equals(providerClassName)) {
                                server = null;
                            }
                        } catch (ServiceConfigurationError e) {
                            final Throwable cause = e.getCause();
                            log.debug(cause.getMessage(), cause);
                            if(SearchServerProviderLoaderException.class.isAssignableFrom(cause.getClass())){
                                final SearchServerProviderLoaderException loaderException = (SearchServerProviderLoaderException) cause;
                                if (loaderException.getServerClass().isAssignableFrom(providerClass)) {
                                    errorMessages.add(loaderException);
                                 }
                             } else if (SearchServerInstantiateException.class.isAssignableFrom(cause.getClass())){
                                final SearchServerInstantiateException instanceException = (SearchServerInstantiateException) cause;
                                errorMessages.add(instanceException);
                             }
                        } catch ( Exception e) {
                            log.debug("Cannot instantiate search server: {}", e.getMessage(), e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Specified Vind Provider class {} is not in classpath",providerClassName, e);
                    throw new SearchServerProviderLoaderException(
                            String.format("Specified class %s is not in classpath", providerClassName),
                            ServiceProvider.class
                    );
                }
            }
        }
        if (server == null) {
            log.error("Unable to found/instantiate SearchServer of class [{}] in classpath", providerClassName);
            final SearchServerException searchServerException = new SearchServerException(
                    "Unable to found/instantiate SearchServer of class [" + providerClassName + "] in classpath");
            errorMessages.forEach(searchServerException::addSuppressed);
            throw searchServerException;
        }
        return server;
    }

    /**
     * Gets a {@link SearchServer} implementation object defined in the classpath from the ServiceLoader.
     * @param collection Name of the core/collection to be used by the instance.
     * @return {@link SearchServer} specific implementation.
     */
    public static SearchServer getInstance(String collection) {

        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, collection);
        return getInstance();
    }

    /**
     * Gets a {@link SearchServer} implementation object defined in the classpath from the ServiceLoader.
     * @param host server which the instance should connect to.
     * @param collection Name of the core/collection to be used by the instance.
     * @return {@link SearchServer} specific implementation.
     */
    public static SearchServer getInstance(String host, String collection) {

        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, host);
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, collection);

        return getInstance();
    }


    /**
     * Gets a {@link SearchServer} implementation object defined in the classpath from the ServiceLoader.
     * @param providerClassName class name of the server provider
     * @param host server which the instance should connect to.
     * @param collection Name of the core/collection to be used by the instance.
     * @return {@link SearchServer} specific implementation.
     */
    public static SearchServer getInstance(String providerClassName,String host, String collection) {

        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, providerClassName);
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, host);
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, collection);

        return getInstance();
    }

    public void indexBean(Object ... t)  {

        List<Document> beanDocuments = new ArrayList<>();

        for (Object bean : t){
            beanDocuments.add(AnnotationUtil.createDocument(bean));
        }

        index(beanDocuments);
    }

    public void indexBean(List<Object> t)  {

        List<Document> beanDocuments = new ArrayList<>();

        for (Object bean : t){
            beanDocuments.add(AnnotationUtil.createDocument(bean));
        }

        index(beanDocuments);
    }

    /**
     * Remove an Object t from the search server index. The object should be annotated at list with
     * {@link com.rbmhtechnology.vind.annotations.Id}.{@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param t Object to be removed from the index.
     * @throws SearchServerException if not possible to perform the deletion.
     * @deprecated use {@link #deleteBean(Object)} instead
     */
    @Deprecated
    public final void delete(Object t) {
        deleteBean(t);
    }

    /**
     * Remove an Object t from the search server index. The object should be annotated at list with
     * {@link com.rbmhtechnology.vind.annotations.Id}.{@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param t Object to be removed from the index.
     * @return {@link DeleteResult} instance containing the deletion execution info.
     * @throws SearchServerException if not possible to perform the deletion.
     */
    public DeleteResult deleteBean(Object t) {
        return delete((Document) AnnotationUtil.createDocument(t));
    }

    /**
     * Gets the specific server client of the server implementation.
     * @return The specific server client.
     */
    public abstract Object getBackend(); //TODO

    public StatusResult getBackendStatus() {
        throw new NotImplementedException("Get Backend Status is not implemened for " + this.getClass().getName());
    }

    /**
     * Adds a {@link Document} or {@link Document}s to the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param doc comma separated {@link Document}s to be indexed.
     */
    public abstract IndexResult index(Document ... doc);

    /**
     * Adds a {@link Document} or {@link Document}s to the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param doc comma separated {@link Document}s to be indexed.
     */
    public abstract IndexResult index(List<Document> doc);

    /**
     * Adds a {@link Document} to the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the index.
     * @param doc comma separated {@link Document}s to be indexed.
     * @param withinMs documents are visible in search within ms
     */
    public abstract IndexResult indexWithin(Document doc, int withinMs);

    /**
     * Adds a {@link Document} or {@link Document}s to the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param doc comma separated {@link Document}s to be indexed.
     * @param withinMs documents are visible in search within ms
     */
    public abstract IndexResult indexWithin(List<Document> doc, int withinMs);

    /**
     * Removes a {@link Document} from the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param doc {@link Document} to be indexed.
     * @return {@link DeleteResult} instance containing the deletion execution info.
     * @throws SearchServerException if not possible to perform the deletion.
     */
    public abstract DeleteResult delete(Document doc);

    /**
     * Removes a {@link Document} from the search server index. {@link SearchServer#commit()} should be executed afterwards for
     * this change to take place on the  index.
     * @param doc {@link Document} to be indexed.
     * @param withinMs documents are visible in search within ms
     * @return {@link DeleteResult} instance containing the deletion execution info.
     * @throws SearchServerException if not possible to perform the deletion.
     */
    public abstract DeleteResult deleteWithin(Document doc, int withinMs);

    /**
     *  Changes a document in the index, based on the modifications described by {@link Update}.
     * @param update {@link Update} modification to do on an specific document in the index.
     * @param factory {@link DocumentFactory} factory with the document schema.
     * @throws SearchServerException if not possible to execute the update.
     */
    public abstract boolean execute(Update update, DocumentFactory factory);

    /**
     * Deletes Documents which match the {@link Delete} filter configuration.
     * @param delete A Delete filter configured.
     * @param factory The type of Document to be deleted.
     * @return {@link DeleteResult} instance containing the deletion execution info.
     */
    public abstract DeleteResult execute(Delete delete, DocumentFactory factory);

    /**
     * Pushes to the index the modifications.
     * @throws SearchServerException if not possible to commit.
     */
    public void commit() {
        this.commit(false);
    }

    /**
     * Pushes to the index the modifications and optionally optimizes.
     * @param optimize boolean flag to indicate whether the core should be optimize after commit or not.
     * @throws SearchServerException if not possible to commit.
     */
    public abstract void commit(boolean optimize);

    /**
     * Executes a {@link FulltextSearch} based on an annotated class.
     * @param search {@link FulltextSearch} search query configuration object.
     * @param c annotated class mapping the index documents and the result type.
     * @param <T> annotated class type.
     * @return {@link BeanSearchResult} storing the search results with type T
     * @throws SearchServerException if not possible to execute the full text search.
     */
    public abstract <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c);

    /**
     * Executes a fulltext search based on an {@link DocumentFactory}.
     * @param search {@link FulltextSearch} search query configuration object.
     * @param factory {@link DocumentFactory} mapping the index documents and the result type.
     * @return {@link SearchResult} storing the search results with type T
     * @throws SearchServerException if not possible to execute the full text search.
     */
    public abstract SearchResult execute(FulltextSearch search, DocumentFactory factory);

    /**
     * Return the raw query sent produced by the server implementation.
     * @param search {@link FulltextSearch} search query configuration object.
     * @param factory {@link DocumentFactory} mapping the index documents and the result type.
     * @return {@link String} Raw query
     */
    public abstract String getRawQuery(FulltextSearch search, DocumentFactory factory);

    /**
     * Return the raw query sent produced by the server implementation.
     * @param search {@link FulltextSearch} search query configuration object.
     * @param c annotated class mapping the index documents and the result type.
     * @param <T> annotated class type.
     * @return {@link String} Raw query
     */
    public abstract <T> String getRawQuery(FulltextSearch search, Class<T> c);

    /**
     * Executes a suggestion search based on an annotated class.
     * @param search ExecutableSuggestionSearch object with the query configuration.
     * @param c Annotated class type to be used as mapping.
     * @param <T> Class type.
     * @return SuggestionResult object storing the server results.
     * @throws SearchServerException if not possible to execute the suggestion search.
     */
    public abstract <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c);

    /**
     * Executes a suggestion search based on an DocumentFactory.
     * @param search ExecutableSuggestionSearch object with the query configuration.
     * @param assets {@link DocumentFactory} mapping the index documents and the result type.
     * @return SuggestionResult object storing the server results.
     * @throws SearchServerException if not possible to execute the suggestion search.
     */
    public abstract SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets);

    /**
     * Executes a suggestion search based on an DocumentFactory and its nested documents of factory childFactory.
     * @param search ExecutableSuggestionSearch object with the query configuration.
     * @param assets {@link DocumentFactory} mapping the index documents and the result type.
     * @param childFactory {@link DocumentFactory} Document factory of the nested documents.
     * @return {@link SuggestionResult} object storing the server results.
     * @throws SearchServerException
     */
    public abstract SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets,DocumentFactory childFactory);

    /**
     * Return the raw query sent produced by the server implementation.
     * @param search {@link ExecutableSuggestionSearch} search query configuration object.
     * @param factory {@link DocumentFactory} mapping the index documents and the result type.
     * @return {@link String} Raw query
     */
    public abstract String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory);

    /**
     * Return the raw query sent produced by the server implementation.
     * @param search {@link ExecutableSuggestionSearch} search query configuration object.
     * @param factory {@link DocumentFactory} mapping the index documents and the result type.
     * @param childFactory {@link DocumentFactory} mapping the index documents children documents.
     * @return {@link String} Raw query
     */
    public abstract String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory);

    /**
     * Return the raw query sent produced by the server implementation.
     * @param search {@link ExecutableSuggestionSearch} search query configuration object.
     * @param c annotated class mapping the index documents and the result type.
     * @param <T> annotated class type.
     * @return {@link String} Raw query
     */
    public abstract <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c);

    /**
     * Executes a get query based on an annotated class.
     * @param search Get object with the query configuration.
     * @param c Annotated class type to be used as mapping.
     * @param <T> Class type.
     * @return SuggestionResult object storing the server results.
     * @throws SearchServerException if not possible to execute the real time get.
     */
    public abstract <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c);
    /**
     * Executes a suggestion search based on an DocumentFactory.
     * @param search ExecutableSuggestionSearch object with the query configuration.
     * @param assets {@link DocumentFactory} mapping the index documents and the result type.
     * @return SuggestionResult object storing the server results.
     * @throws SearchServerException if not possible to execute the real time get.
     */
    public abstract GetResult execute(RealTimeGet search, DocumentFactory assets);

    /**
     * Finds the matching queries for a given document. {@link InverseSearch}.
     * @param inverseSearch {@link InverseSearch} definition of the inverse search.
     * @param factory {@link DocumentFactory} mapping the index documents and the result type.
     * @return {@link SearchResult} storing the search results matching the inverse search.
     * @throws SearchServerException if not possible to execute the update.
     */
    public abstract InverseSearchResult execute(InverseSearch inverseSearch, DocumentFactory factory);

    /**
     * Adds a {@link InverseSearchQuery} to the index. If the query already exists it will be updated.
     * @param query {@link InverseSearchQuery} to be stored.
     * @return {@link IndexResult} containing the time information.
     */
    public abstract IndexResult addInverseSearchQuery(InverseSearchQuery query);

    /**
     * Deletes all elements on the current index.
     */
    public abstract void clearIndex();

    /**
     * Closes connection to server.
     * @throws SearchServerException if not possible to close connection to the server.
     */
    public abstract void close();

    public abstract Class<? extends ServiceProvider> getServiceProviderClass();

    public abstract void closeCursor(String cursor);
}
