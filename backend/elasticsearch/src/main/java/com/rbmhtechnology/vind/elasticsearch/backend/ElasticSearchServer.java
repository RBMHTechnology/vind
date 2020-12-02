package com.rbmhtechnology.vind.elasticsearch.backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.SearchServerInstantiateException;
import com.rbmhtechnology.vind.SearchServerProviderLoaderException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SmartSearchServerBase;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearchQueryFactory;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.result.BeanGetResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.DeleteResult;
import com.rbmhtechnology.vind.api.result.FacetResults;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.InverseSearchPageResult;
import com.rbmhtechnology.vind.api.result.InverseSearchResult;
import com.rbmhtechnology.vind.api.result.InverseSearchSliceResult;
import com.rbmhtechnology.vind.api.result.PageResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SliceResult;
import com.rbmhtechnology.vind.api.result.StatusResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.elasticsearch.backend.util.DocumentUtil;
import com.rbmhtechnology.vind.elasticsearch.backend.util.ElasticMappingUtils;
import com.rbmhtechnology.vind.elasticsearch.backend.util.ElasticQueryBuilder;
import com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil;
import com.rbmhtechnology.vind.elasticsearch.backend.util.PainlessScript;
import com.rbmhtechnology.vind.elasticsearch.backend.util.ResultUtils;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.Asserts;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkItemResponse.Failure;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.DocumentUtil.createEmptyDocument;

public class ElasticSearchServer extends SmartSearchServerBase {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchServer.class);
    private static final Logger elasticClientLogger = LoggerFactory.getLogger(log.getName() + "#elasticSearchClient");

    private ServiceProvider serviceProviderClass;
    private final ElasticVindClient elasticSearchClient;

    public ElasticSearchServer() {
        // this is mainly used with the ServiceLoader infrastructure
        this(getElasticServerProvider() != null ? getElasticServerProvider().getInstance() : null);
        serviceProviderClass = getElasticServerProvider();
    }

    /**
     * Creates an instance of ElasticSearch server performing ping and the schema validity check.
     * @param client ElasticClient to connect to.
     */
    public ElasticSearchServer(ElasticVindClient client) {
        this(client, true);
    }

    /**
     * Creates an instance of ElasticSearch server allowing to avoid the schema validity check.
     * @param client ElasticClient to connect to.
     * @param check true to perform local schema validity check against remote schema, false otherwise.
     */
    protected ElasticSearchServer(ElasticVindClient client, boolean check) {

        elasticSearchClient = client;

        //In order to perform unit tests with mocked ElasticClient, we do not need to do the schema check.
        if(check){
            if(client != null) {
                try {
                    if (elasticSearchClient.ping()) {
                        log.debug("Successful ping to Elasticsearch server");
                    } else {
                        log.error("Cannot connect to Elasticsearch server: ping failed");
                        throw new SearchServerInstantiateException("Cannot connect to Elasticsearch server: ping failed", this.getClass());
                    }
                } catch ( IOException e) {
                    log.error("Cannot connect to Elasticsearch server: ping failed");
                    throw new SearchServerInstantiateException("Cannot connect to Elasticsearch server: ping failed", this.getClass(), e);
                }
                log.info("Ping to Elastic server successful");

                try {
                    if(!client.indexExists()) {
                        if(SearchConfiguration.get(SearchConfiguration.SERVER_COLLECTION_AUTOCREATE, false)) {
                            log.debug("Collection {} does not exist in elasticsearch host: trying to auto-create.", client.getDefaultIndex());
                            try {
                                log.info("Creating elastic collection {}", client.getDefaultIndex());
                                client.createIndex(client.getDefaultIndex());
                            } catch (Exception e) {
                                log.error("Error creating collection {}: {}", client.getDefaultIndex(), e.getMessage(), e);
                                throw new SearchServerInstantiateException(
                                        String.format(
                                                "Error when creating collection %s: %s",
                                                client.getDefaultIndex(), e.getMessage()
                                        ),
                                        this.getClass(),
                                        e
                                );
                            }
                            log.info("Collection {} created successfully", client.getDefaultIndex());
                        } else {
                            log.error("Index does not exist: try to enable auto-creation");
                            throw new SearchServerInstantiateException(
                                    "Index does not exist: try to enable auto-creation",
                                    this.getClass());
                        }
                    }
                } catch (Exception e) {
                    log.error("Cannot connect to Elasticsearch server: index check failed - {}",e.getMessage(), e);
                    throw new SearchServerInstantiateException(
                            "Cannot connect to Elasticsearch server: index check failed - " + e.getMessage(),
                            this.getClass(),
                            e);
                }

                try {
                    checkVersionAndMappings();
                } catch (Exception e) {
                    log.error("Elasticsearch server Schema validation error: {}", e.getMessage(), e);
                    throw new SearchServerInstantiateException(
                            String.format("Elastic search Schema validation error: %s", e.getMessage()),
                            this.getClass(),
                            e);
                }
            } else {
                log.error("Error running Elasticsearch Search Server: search server instance is null");
                throw new SearchServerInstantiateException(
                        "Error running Elasticsearch Search Server: Elasticsearch client is null",
                        this.getClass());
            }
          } else {
            log.warn("Elastic ping and schema validity check has been deactivated.");
        }
    }

    private void checkVersionAndMappings() throws IOException {

         final TypeReference<HashMap<String, Object>> typeRef =
                 new TypeReference<HashMap<String, Object>>() {};
        final Map<String, Object> localMappings = new ObjectMapper().readValue(ElasticMappingUtils.getDefaultMapping(), typeRef);
        final Map<String, Object> remoteMappings = elasticSearchClient.getMappings().mappings().get(elasticSearchClient.getDefaultIndex()).getSourceAsMap();

        ElasticMappingUtils.checkMappingsCompatibility(localMappings,remoteMappings,elasticSearchClient.getDefaultIndex());
    }

    @Override
    public Object getBackend() {
        return elasticSearchClient;
    }

    @Override
    public StatusResult getBackendStatus() {
        try {
            if(elasticSearchClient.ping()) {
                return StatusResult.up().setDetail("status", 0);
            } else {
                return StatusResult.down().setDetail("status", 1);
            }

        } catch ( IOException e) {
            log.error("Cannot connect to Elasticsearch server: ping failed");
            throw new SearchServerException("Cannot connect to Elasticsearch server: ping failed", e);
        }
    }

    @Override
    public IndexResult index(Document... docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(docs.length > 0, "Should be at least one document to index.");
        return indexMultipleDocuments(Arrays.asList(docs), -1);
    }

    @Override
    public IndexResult index(List<Document> docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(!docs.isEmpty(), "Should be at least one document to index.");

        return  indexMultipleDocuments(docs, -1);
    }

    @Override
    public IndexResult indexWithin(Document doc, int withinMs) {
        Asserts.notNull(doc,"Document to index should not be null.");
        log.debug("Parameter 'within' not in use in elastic search backend");
        return indexMultipleDocuments(Collections.singletonList(doc), withinMs);
    }

    @Override
    public IndexResult indexWithin(List<Document> docs, int withinMs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(!docs.isEmpty(), "Should be at least one document to index.");
        log.debug("Parameter 'within' not in use in elastic search backend");
        return  indexMultipleDocuments(docs, withinMs);
    }

    @Override
    public DeleteResult delete(Document doc) {
        return deleteWithin(doc, -1);
    }

    @Override
    public DeleteResult deleteWithin(Document doc, int withinMs) {
        log.debug("Parameter 'within' not in use in elastic search backend");
        try {
            final StopWatch elapsedTime = StopWatch.createStarted();
            elasticClientLogger.debug(">>> delete({})", doc.getId());
            final DeleteResponse deleteResponse = elasticSearchClient.deleteById(doc.getId());
            if(deleteResponse.status().getStatus() == 404) {
                log.warn("Document which should be deleted does not exist");
            } else if(deleteResponse.status().getStatus() >= 400) {
                log.error("Cannot delete document {}: {} - {} ", doc.getId(), deleteResponse.status().getStatus(),deleteResponse.status().name());
                throw new SearchServerException("Cannot  delete document " + doc.getId() + ": " + deleteResponse.status().getStatus() +" - "+ deleteResponse.status().name());
            }
            elapsedTime.stop();
            return new DeleteResult(elapsedTime.getTime()).setElapsedTime(elapsedTime.getTime());
        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot delete document {}", doc.getId() , e);
            throw new SearchServerException("Cannot delete document "+ doc.getId(), e);
        }
    }

    @Override
    public boolean execute(Update update, DocumentFactory factory) {
        try {
            log.debug("Update script builder does not check for script injection. Ensure values provided are script safe.");
            final StopWatch elapsedTime = StopWatch.createStarted();
            elasticClientLogger.debug(">>> update({})", update);
            final PainlessScript.ScriptBuilder updateScript = ElasticQueryBuilder.buildUpdateScript(update.getOptions(), factory, update.getUpdateContext());
            final UpdateResponse response = elasticSearchClient.update(update.getId(), updateScript);
            if(response.status().getStatus() >= 400) {
                log.error("Cannot update document {}: {} - {} ", update.getId(), response.status().getStatus(),response.status().name());
                throw new SearchServerException("Cannot  update document " + update.getId() + ": " + response.status().getStatus() +" - "+ response.status().name());
            }
            elapsedTime.stop();
            return true;
        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot update document {}: {}", update.getId(), e.getMessage() , e);
            throw new SearchServerException(
                    String.format("Cannot update document %s: %s", update.getId(), e.getMessage()), e);
        }
    }

    @Override
    public DeleteResult execute(Delete delete, DocumentFactory factory) {
        try {
            createDocumentFactoryFootprint(factory);
            final StopWatch elapsedTime = StopWatch.createStarted();
            elasticClientLogger.debug(">>> delete({})", delete);
            final QueryBuilder deleteQuery = ElasticQueryBuilder.buildFilterQuery(delete.getQuery(), factory, delete.getUpdateContext());
            final BulkByScrollResponse response = elasticSearchClient.deleteByQuery(deleteQuery);
            if(response.getBulkFailures().size() > 0) {
                final List<String> failureMessages = response.getBulkFailures().stream()
                        .map(Failure::getMessage)
                        .collect(Collectors.toList());
                log.error("Cannot delete {} documents: {}",failureMessages.size() , String.join(" - ", failureMessages));
                throw new SearchServerException("Cannot delete " + failureMessages.size() + "documents: " + String.join(" - ", failureMessages));

            }
            elapsedTime.stop();
            return new DeleteResult(response.getTook().getMillis()).setElapsedTime(elapsedTime.getTime());
        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot delete with query {}", delete.getQuery() , e);
            throw new SearchServerException(
                    String.format("Cannot delete with query %s", delete.getQuery().toString()), e);
        }
    }

    @Override
    public void commit(boolean optimize) {
        log.debug("Commit does not have any effect on elastic search backend");
    }

    @Override
    protected  <T> BeanSearchResult<T> doExecute(FulltextSearch search, Class<T> c) {
        final DocumentFactory factory = AnnotationUtil.createDocumentFactory(c);
        createDocumentFactoryFootprint(factory);

        final SearchResult docResult = this.execute(search, factory);
        return docResult.toPojoResult(docResult, c);
    }

    @Override
    protected SearchResult doExecute(FulltextSearch search, DocumentFactory factory) {
        createDocumentFactoryFootprint(factory);
        final StopWatch elapsedtime = StopWatch.createStarted();

        //query
        try {
            final String searchString = search.getSearchString();
            final ValidateQueryResponse validateQueryResponse = elasticSearchClient.validateQuery(searchString);
            if (validateQueryResponse.isValid()) {
                search.text(searchString);
            }
            final SearchSourceBuilder query = ElasticQueryBuilder.buildQuery(search, factory, !validateQueryResponse.isValid());
            elasticClientLogger.debug(">>> query({})", query.toString());
            final SearchResponse response = elasticSearchClient.query(query);
            if(Objects.nonNull(response)
                    && Objects.nonNull(response.getHits())
                    && Objects.nonNull(response.getHits().getHits())){

                final List<Document> documents = Arrays.stream(response.getHits().getHits())
                        .map(hit -> DocumentUtil.buildVindDoc(hit, factory, search.getSearchContext()))
                        .collect(Collectors.toList());

                if ( search.isSpellcheck()
                        && CollectionUtils.isEmpty(documents)) {

                    //if no results, try spellchecker (if defined and if spellchecked query differs from original)
                    final List<String> spellCheckedQuery = ElasticQueryBuilder.getSpellCheckedQuery(response);

                    //query with checked query

                    if(spellCheckedQuery != null && CollectionUtils.isNotEmpty(spellCheckedQuery)) {
                        final Iterator<String> iterator = spellCheckedQuery.iterator();
                        while(iterator.hasNext()) {
                            final String text = iterator.next();
                            final FulltextSearch spellcheckSearch = search.copy().text(text).spellcheck(false);
                            final SearchSourceBuilder spellcheckQuery =
                                    ElasticQueryBuilder.buildQuery(spellcheckSearch, factory);
                            final SearchResponse spellcheckResponse = elasticSearchClient.query(spellcheckQuery);
                            documents.addAll(Arrays.stream(spellcheckResponse.getHits().getHits())
                                    .map(hit -> DocumentUtil.buildVindDoc(hit, factory, search.getSearchContext()))
                                    .collect(Collectors.toList()));
                        }
                    }
                }

                // Building Vind Facet Results
                final FacetResults facetResults =
                        ResultUtils.buildFacetResults(
                                response.getAggregations(),
                                factory,
                                search.getFacets(),
                                search.getSearchContext());

                elapsedtime.stop();

                final long totalHits = response.getHits().getTotalHits().value;
                switch(search.getResultSet().getType()) {
                    case page:{
                        return new PageResult(totalHits, response.getTook().getMillis(), documents, search, facetResults, this, factory).setElapsedTime(elapsedtime.getTime());
                    }
                    case slice: {
                        return new SliceResult(totalHits, response.getTook().getMillis(), documents, search, facetResults, this, factory).setElapsedTime(elapsedtime.getTime());
                    }
                    default:
                        return new PageResult(totalHits, response.getTook().getMillis(), documents, search, facetResults, this, factory).setElapsedTime(elapsedtime.getTime());
                }
            }else {
                throw new ElasticsearchException("Empty result from ElasticClient");
            }

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue query: %s",e.getMessage()), e);
        }
    }

    @Override
    public String getRawQuery(FulltextSearch search, DocumentFactory factory) {
        final SearchSourceBuilder query = ElasticQueryBuilder.buildQuery(search, factory);
        return query.toString();
    }

    @Override
    public <T> String getRawQuery(FulltextSearch search, Class<T> c) {
        return getRawQuery(search, AnnotationUtil.createDocumentFactory(c));
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        throw new NotImplementedException();
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory factory) {
        createDocumentFactoryFootprint(factory);
        final StopWatch elapsedtime = StopWatch.createStarted();
        final SearchSourceBuilder query = ElasticQueryBuilder.buildSuggestionQuery(search, factory);
        //query
        try {
            elasticClientLogger.debug(">>> query({})", query.toString());
            final SearchResponse response = elasticSearchClient.query(query);
            HashMap<FieldDescriptor, TermFacetResult<?>> suggestionValues =
                    ResultUtils.buildSuggestionResults(response, factory, search.getSearchContext());

            String spellcheckText = null;
            if (!suggestionValues.values().stream()
                    .anyMatch(termFacetResult -> CollectionUtils.isNotEmpty(termFacetResult.getValues())) ) {

                //if no results, try spellchecker (if defined and if spellchecked query differs from original)
                final List<String> spellCheckedQuery = ElasticQueryBuilder.getSpellCheckedQuery(response);

                //query with checked query

                if(spellCheckedQuery != null && CollectionUtils.isNotEmpty(spellCheckedQuery)) {
                    final Iterator<String> iterator = spellCheckedQuery.iterator();
                    while(iterator.hasNext()) {
                        final String text = iterator.next();
                        final SearchSourceBuilder spellcheckQuery = ElasticQueryBuilder.buildSuggestionQuery(search.text(text), factory);
                        final SearchResponse spellcheckResponse = elasticSearchClient.query(spellcheckQuery);
                        final HashMap<FieldDescriptor, TermFacetResult<?>> spellcheckValues =
                                ResultUtils.buildSuggestionResults(spellcheckResponse, factory, search.getSearchContext());
                        if (spellcheckValues.values().stream()
                                .anyMatch(termFacetResult -> CollectionUtils.isNotEmpty(termFacetResult.getValues())) ) {
                            spellcheckText = text;
                            suggestionValues = spellcheckValues;
                            break;
                        }
                    }
                }
            }

            elapsedtime.stop();
            return new SuggestionResult(
                    suggestionValues,
                    spellcheckText,
                    response.getTook().getMillis(),
                    factory)
                    .setElapsedTime(elapsedtime.getTime(TimeUnit.MILLISECONDS));

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue query: %s",e.getMessage()), e);
        }
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        throw new NotImplementedException();
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {
        final SearchSourceBuilder query = ElasticQueryBuilder.buildSuggestionQuery(search, factory);
        return query.toString();
    }

    @Override
    public <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c) {
        return getRawQuery(search,AnnotationUtil.createDocumentFactory(c));
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory) {
        throw new NotImplementedException();
    }

    @Override
    public <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c) {
        final DocumentFactory documentFactory = AnnotationUtil.createDocumentFactory(c);
        createDocumentFactoryFootprint(documentFactory);
        final GetResult result = this.execute(search, documentFactory);
        return result.toPojoResult(result,c);
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        try {
            final StopWatch elapsedTime = StopWatch.createStarted();
            createDocumentFactoryFootprint(assets);
            final MultiGetResponse response = elasticSearchClient.realTimeGet(search.getValues());
            elapsedTime.stop();

            if(response!=null){
                return ResultUtils.buildRealTimeGetResult(response, search, assets, elapsedTime.getTime()).setElapsedTime(elapsedTime.getTime());
            }else {
                log.error("Null result from ElasticClient");
                throw new SearchServerException("Null result from ElasticClient");
            }

        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot execute realTime get query");
            throw new SearchServerException("Cannot execute realTime get query", e);
        }
    }

    @Override
    public InverseSearchResult execute(InverseSearch inverseSearch, DocumentFactory documentFactory) {
        final StopWatch elapsedtime = StopWatch.createStarted();
        final List<Map<String,Object>> mapDocs = inverseSearch.getDocs().parallelStream()
                .map(DocumentUtil::createInputDocument)
                .collect(Collectors.toList());
        final QueryBuilder query =
                ElasticQueryBuilder.buildFilterQuery(inverseSearch.getQueryFilter(), documentFactory, null);
        //query
        try {
            elasticClientLogger.debug(">>> query({})", query.toString());
            //TODO: inverse search support multiple docs
            final SearchResponse response = elasticSearchClient.percolatorDocQuery(mapDocs, query);

            if(Objects.nonNull(response)
                    && Objects.nonNull(response.getHits())
                    && Objects.nonNull(response.getHits().getHits())){

                final List<InverseSearchQuery> resultQueries = Arrays.stream(response.getHits().getHits())
                        .map(hit -> DocumentUtil.buildVindDoc(hit, InverseSearchQueryFactory.getQueryFactory(),null))
                        .map(doc ->
                                documentFactory.createInverseSearchQuery(
                                        doc.getId(),
                                        deserializeByteArrayFilter(((ByteBuffer)doc.getValue(InverseSearchQueryFactory.BINARY_QUERY_FIELD)).array()))

                        )
                        .collect(Collectors.toList());


                elapsedtime.stop();

                final long totalHits = response.getHits().getTotalHits().value;
                switch(inverseSearch.getResultSet().getType()) {
                    case page:{
                        return new InverseSearchPageResult(totalHits, response.getTook().getMillis(), resultQueries, inverseSearch, this, documentFactory).setElapsedTime(elapsedtime.getTime());
                    }
                    case slice: {
                        return new InverseSearchSliceResult(totalHits, response.getTook().getMillis(), resultQueries, inverseSearch, this, documentFactory).setElapsedTime(elapsedtime.getTime());
                    }
                    default:
                        return new InverseSearchPageResult(totalHits, response.getTook().getMillis(), resultQueries, inverseSearch,  this, documentFactory).setElapsedTime(elapsedtime.getTime());
                }
            }else {
                throw new ElasticsearchException("Empty result from ElasticClient");
            }

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue inverse search: %s", e.getMessage()), e);
        }

    }

    @Override
    public IndexResult addInverseSearchQuery(InverseSearchQuery query) {
        Asserts.notNull(query,"Query should not be null.");
        final StopWatch elapsedTime = StopWatch.createStarted();

        createDocumentFactoryFootprint(query.getFactory());

        final QueryBuilder elasticQuery =
                ElasticQueryBuilder.buildFilterQuery(query.getQuery(), query.getFactory(), null);

        try {
            if (elasticClientLogger.isTraceEnabled()) {
                elasticClientLogger.debug(">>> add inverse search Query({})", query);
            } else {
                elasticClientLogger.debug(">>> add inverse search Query({})", query);
            }
            final Map<String, Object> metadataMap = new HashMap<>();

            if (!query.getValues().isEmpty()) {
                 metadataMap.putAll(DocumentUtil.createInputDocument(query));
            }

            final BulkResponse response = this.elasticSearchClient.addPercolateQuery(query.getId(), elasticQuery, metadataMap) ;
            elapsedTime.stop();
            return new IndexResult(elapsedTime.getTime()).setElapsedTime(elapsedTime.getTime());

        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot add inverse search query {}", query, e);
            throw new SearchServerException("Cannot  add inverse search query", e);
        }
    }

    @Override
    public void clearIndex() {
        try {
            elasticClientLogger.debug(">>> clear complete index");
            elasticSearchClient.deleteByQuery(QueryBuilders.matchAllQuery());
        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot clear index", e);
            throw new SearchServerException("Cannot clear index", e);
        }
    }

    @Override
    public void close() {
        if (elasticSearchClient != null) try {
            elasticSearchClient.close();
        } catch (IOException e) {
            log.error("Cannot close search server", e);
            throw new SearchServerException("Cannot close search server", e);
        }
    }

    @Override
    public Class<? extends ServiceProvider> getServiceProviderClass() {
        return ElasticServerProvider.class;
    }

    private static ElasticServerProvider getElasticServerProvider() {
        final String providerClassName = SearchConfiguration.get(SearchConfiguration.SERVER_PROVIDER, null);

        final ServiceLoader<ElasticServerProvider> loader = ServiceLoader.load(ElasticServerProvider.class);
        final Iterator<ElasticServerProvider> it = loader.iterator();

        ElasticServerProvider serverProvider = null;
        if(providerClassName == null) {
            if (!it.hasNext()) {
                log.error("No ElasticServerProvider in classpath");
                throw new SearchServerException("No ElasticServerProvider in classpath");
            } else {
                serverProvider = it.next();
            }
        } else {
            try {
                final Class<?> providerClass = Class.forName(providerClassName);
                if (ElasticServerProvider.class.isAssignableFrom(providerClass)){
                    while(it.hasNext()) {
                        final ElasticServerProvider p = it.next();
                        if(providerClass.isAssignableFrom(p.getClass())) {
                            serverProvider = p;
                            break;
                        }
                    }
                    if(Objects.isNull(serverProvider)) {
                        log.debug("No Elastic server provider of type class {} found in classpath for server {}",
                                providerClassName, ElasticSearchServer.class.getCanonicalName());
                    }

                } else {
                    log.debug("Search server provider class {} configured is not assignable to {}",
                            providerClassName, ElasticServerProvider.class.getCanonicalName());
                    throw new SearchServerProviderLoaderException(
                            String.format("Search server provider class %s configured is not assignable to %s",providerClassName, ElasticServerProvider.class.getCanonicalName()),
                            ElasticServerProvider.class
                    );
                }
            } catch (ClassNotFoundException e) {
                log.warn("Specified Vind Provider class {} is not in classpath",providerClassName, e);
                throw new SearchServerProviderLoaderException(
                        String.format("Specified class %s is not in classpath", providerClassName),
                        ElasticServerProvider.class
                );
            }
        }

        return serverProvider;
    }

    private IndexResult indexSingleDocument(Document doc, int withinMs) {
        log.debug("Parameter 'within' not in use in elastic search backend");
        final StopWatch elapsedTime = StopWatch.createStarted();
        final Map<String,Object> document = DocumentUtil.createInputDocument(doc);

        try {
            if (elasticClientLogger.isTraceEnabled()) {
                elasticClientLogger.debug(">>> add({})", doc.getId());
            } else {
                elasticClientLogger.debug(">>> add({})", doc.getId());
            }

            final BulkResponse response = this.elasticSearchClient.add(document);
            if(response.hasFailures()) {
                final List<String> failureMessages = Stream.of(response.getItems())
                        .filter(BulkItemResponse::isFailed)
                        .map(BulkItemResponse::getFailureMessage)
                        .collect(Collectors.toList());
                log.error("Cannot index document {}: {}", document.get(FieldUtil.ID) , failureMessages.get(0));
                throw new SearchServerException("Cannot index document " + document.get(FieldUtil.ID) + ": " + failureMessages.get(0));

            }
            elapsedTime.stop();
            return new IndexResult(response.getTook().getMillis()).setElapsedTime(elapsedTime.getTime());

        } catch (ElasticsearchException | IOException e) {
            log.error("Cannot index document {}", document.get(FieldUtil.ID) , e);
            throw new SearchServerException("Cannot index document", e);
        }
    }

    private IndexResult indexMultipleDocuments(List<Document> docs, int withinMs) {
        log.debug("Parameter 'within' not in use in elastic search backend");
        final StopWatch elapsedTime = StopWatch.createStarted();
        final List<Map<String,Object>> jsonDocs = docs.parallelStream()
                .map(DocumentUtil::createInputDocument)
                .collect(Collectors.toList());
        try {
            if (elasticClientLogger.isTraceEnabled()) {
                elasticClientLogger.debug(">>> add({})", jsonDocs);
            } else {
                elasticClientLogger.debug(">>> add({})", jsonDocs);
            }

            final BulkResponse response =this.elasticSearchClient.add(jsonDocs) ;
            if(response.hasFailures()) {
                final List<String> failureMessages = Stream.of(response.getItems())
                        .filter(BulkItemResponse::isFailed)
                        .map(BulkItemResponse::getFailureMessage)
                        .collect(Collectors.toList());
                log.error("Cannot index {} documents: {}",failureMessages.size() , String.join(" - ", failureMessages));
                throw new SearchServerException("Cannot index " + failureMessages.size() + "documents: " + String.join(" - ", failureMessages));

            }
            elapsedTime.stop();
            return new IndexResult(elapsedTime.getTime()).setElapsedTime(elapsedTime.getTime());

        } catch (ElasticsearchException | IOException e) {
            log.error("Error indexing {} documents [{}]: {}",
                    docs.size(),
                    docs.stream().map(Document::getId).collect(Collectors.joining(", ")),
                    e.getMessage(), e);
            throw new SearchServerException(
                    String.format(
                            "Error indexing %s documents [%s]: %s",
                                docs.size(),
                                docs.stream().map(Document::getId).collect(Collectors.joining(", ")),
                                e.getMessage()), e);
        }
    }

    private Filter deserializeByteArrayFilter(byte[] data) {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(data);
        try(ObjectInputStream ois = new ObjectInputStream(bytesIn))
        {
            Filter query = (Filter) ois.readObject();
            return query;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error desearializing byte[] filter: "+e.getMessage(),e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error desearializing byte[] filter: "+e.getMessage(),e);
        }
    }

    private void createDocumentFactoryFootprint(DocumentFactory factory) {
        final SearchSourceBuilder query = ElasticQueryBuilder.buildPercolatorQueryReadiness(factory);

        try {
            addExistingContextsToDescriptors(factory);
            elasticClientLogger.debug(">>> query({})", query.toString());
            final SearchResponse response = elasticSearchClient.query(query);

            if(Objects.nonNull(response)
                    && Objects.nonNull(response.getHits())
                    && Objects.nonNull(response.getHits().getHits())){

                final List<Document> documents = Arrays.stream(response.getHits().getHits())
                        .map(hit -> DocumentUtil.buildVindDoc(hit, factory, null))
                        .collect(Collectors.toList());

                final Map<String, Object> emptyDocument = createEmptyDocument(factory);
                if (CollectionUtils.isEmpty(documents)
                        || !DocumentUtil.equalDocs(documents.get(0), emptyDocument, factory)){
                    this.elasticSearchClient.add(emptyDocument);
                }

            }else {
                throw new ElasticsearchException("Empty result from ElasticClient");
            }

        } catch (ElasticsearchException | IOException e) {
            throw new SearchServerException(String.format("Cannot issue query: %s",e.getMessage()), e);
        }
    }

    private void addExistingContextsToDescriptors(DocumentFactory factory) throws IOException {
        final String pattern = "(" + FieldUtil.INTERNAL_FIELD_PREFIX + ")|(" + FieldUtil.INTERNAL_COMPLEX_FIELD_PREFIX + ")";
        final List<String> contextFields = ((Map<String, Object>) elasticSearchClient.getMappings().mappings().get(elasticSearchClient.getDefaultIndex()).getSourceAsMap().get("properties")).keySet().stream()
                .filter(fieldName -> fieldName.startsWith("dynamic_") || fieldName.startsWith("context_"))
                .map(fieldName -> Pattern.compile(pattern).matcher(fieldName).replaceFirst(""))
                .filter( name -> name.contains("_"))
                .collect(Collectors.toList());
        contextFields.stream()
                .map( fieldName -> fieldName.split("_", 2))
                .filter( splitedField -> factory.hasField(splitedField[1]))
                .forEach( fieldcontext -> factory.getField(fieldcontext[1]).setContextualized(true, fieldcontext[0]) );
    }


}
