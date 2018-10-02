package com.rbmhtechnology.vind.solr.backend;

import com.google.common.io.Resources;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.delete.Delete;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.query.suggestion.DescriptorSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.StringSuggestionSearch;
import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.query.update.Update.UpdateOperations;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.api.result.*;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import com.rbmhtechnology.vind.utils.FileSystemUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Asserts;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.*;
import org.apache.solr.common.util.DateUtil;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.api.query.update.Update.UpdateOperations.set;
import static com.rbmhtechnology.vind.solr.backend.SolrUtils.Fieldname.*;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 21.06.16.
 */
public class SolrSearchServer extends SearchServer {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchServer.class);
    private static final Logger solrClientLogger = LoggerFactory.getLogger(log.getName() + "#solrClient");
    public static final String SOLR_WILDCARD = "*";
    public static final String SUGGESTION_DF_FIELD = "suggestions";

    private ServiceProvider serviceProviderClass;
    private final SolrClient solrClient;

    public SolrSearchServer() {
        // this is mainly used with the ServiceLoader infrastructure
        this(getSolrServerProvider() != null ? getSolrServerProvider().getInstance() : null);
        serviceProviderClass = getSolrServerProvider();

    }

    public SolrSearchServer(SolrClient client) {
        this(client, true);
    }

    /**
     * Creates an instance of SolrSearch server allowing to avoid the schema validity check.
     * @param client SolrClient to connect to.
     * @param check true to perform local schema validity check against remote schema, false otherwise.
     */
    protected SolrSearchServer(SolrClient client, boolean check) {
        solrClient = client;

        //In order to perform unit tests with mocked solrClient, we do not need to do the schema check.
        if(check && client != null) {
            try {
                final SolrPingResponse ping = solrClient.ping();
                if (ping.getStatus() == 0) {
                    log.debug("Pinged Solr in {}", ping.getQTime());
                }
            } catch (SolrServerException | IOException e) {
                log.error("Cannot connect to solr server", e);
                throw new RuntimeException();
            }
            log.info("Connection to solr server successful");

            checkVersionAndSchema();
        } else {
            log.warn("Solr ping and schema validity check has been deactivated.");
        }
    }

    private void checkVersionAndSchema() {
        //check schema
        try {
            final SchemaResponse response = new SchemaRequest().process(solrClient);
            final Path localSchema = FileSystemUtils.toPath(Resources.getResource("solrhome/core/conf/schema.xml"));
            SolrSchemaChecker.checkSchema(localSchema, response);
        } catch (SolrServerException e) {
            log.error("Cannot get schema for solr client", e);
            throw new RuntimeException(e);
        } catch (URISyntaxException | IOException e) {
            log.error("Cannot read schema", e);
            throw new RuntimeException(e);
        } catch (SchemaValidationException e) {
            log.error("Schema is not valid for library", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object getBackend() {
        return solrClient;
    }

    @Override
    public StatusResult getBackendStatus() {
        CoreAdminRequest request = new CoreAdminRequest();
        request.setAction(CoreAdminParams.CoreAdminAction.STATUS);
        try {
            CoreAdminResponse response = request.process(this.solrClient);
            int statusCode = response.getStatus();

            if(statusCode != 0) {
                return StatusResult.down().setDetail("status", statusCode);
            } else {
                return StatusResult.up().setDetail("status", statusCode);
            }

        } catch (SolrServerException | IOException e) {
            log.error("Cannot ping server");
            throw new SearchServerException("Cannot ping server", e);
        }
    }

    @Override
    public IndexResult index(Document ... docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(docs.length > 0, "Should be at least one document to index.");
        return indexMultipleDocuments(Arrays.asList(docs), -1);
    }

    @Override
    public IndexResult index(List<Document> docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(docs.size() > 0, "Should be at least one document to index.");

        return  indexMultipleDocuments(docs, -1);
    }

    @Override
    public IndexResult indexWithin(Document doc, int withinMs) {
        return this.indexSingleDocument(doc, withinMs);
    }

    @Override
    public IndexResult indexWithin(List<Document> doc, int withinMs) {
        return this.indexMultipleDocuments(doc, withinMs);
    }

    private IndexResult indexSingleDocument(Document doc, int withinMs) {
        final SolrInputDocument document = createInputDocument(doc);
        try {
            if (solrClientLogger.isTraceEnabled()) {
                solrClientLogger.debug(">>> add({}): {}", doc.getId(), ClientUtils.toXML(document));
            } else {
                solrClientLogger.debug(">>> add({})", doc.getId());
            }

            removeNonParentDocument(doc, withinMs);
            final UpdateResponse response = withinMs < 0 ? this.solrClient.add(document) : this.solrClient.add(document, withinMs);
            return new IndexResult(Long.valueOf(response.getQTime())).setElapsedTime(response.getElapsedTime());

        } catch (SolrServerException | IOException e) {
            log.error("Cannot index document {}", document.getField(ID) , e);
            throw new SearchServerException("Cannot index document", e);
        }
    }

    private IndexResult indexMultipleDocuments(List<Document> docs, int withinMs) {
        final List<SolrInputDocument> solrDocs = docs.parallelStream()
                .map(doc -> createInputDocument(doc))
                .collect(Collectors.toList());
        try {
            if (solrClientLogger.isTraceEnabled()) {
                solrClientLogger.debug(">>> add({})", solrDocs);
            } else {
                solrClientLogger.debug(">>> add({})", solrDocs);
            }
            for(Document doc : docs){
                removeNonParentDocument(doc, withinMs);
            }

            final UpdateResponse response = withinMs < 0 ? this.solrClient.add(solrDocs) : this.solrClient.add(solrDocs, withinMs);
            return new IndexResult(Long.valueOf(response.getQTime())).setElapsedTime(response.getElapsedTime());

        } catch (SolrServerException | IOException e) {
            log.error("Cannot index documents {}", solrDocs, e);
            throw new SearchServerException("Cannot index documents", e);
        }
    }

    private void removeNonParentDocument(Document doc, int withinMs) throws SolrServerException, IOException {
        if(CollectionUtils.isNotEmpty(doc.getChildren())) {
            //Get the nested docs of the document if existing
            final NamedList<Object> paramList = new NamedList<>();
            paramList.add(CommonParams.Q, "!( _id_:"+ doc.getId()+")&(_root_:"+ doc.getId()+")");
            final QueryResponse query = solrClient.query(SolrParams.toSolrParams(paramList), SolrRequest.METHOD.POST);
            if (CollectionUtils.isEmpty(query.getResults()))
                log.info("Deleting document `{}`: document is becoming parent.",doc.getId());

            if(withinMs < 0) {
                this.solrClient.deleteById(doc.getId());
            } else {
                this.solrClient.deleteById(doc.getId(), withinMs);
            }
        }
    }

    private SolrInputDocument createInputDocument(Document doc) {
        final SolrInputDocument document = new SolrInputDocument();
        //add fields
        doc.listFieldDescriptors()
                .values()
                .stream()
                .filter(doc::hasValue)
                //TODO: move again to an approach where we do not go through all the use cases but based on which flags the descriptor has set to true
                .forEach(descriptor ->
                        doc.getFieldContexts(descriptor).stream().forEach(context ->
                                Stream.of(UseCase.values())
                                        .forEach(useCase -> {
                                                    final String fieldname = getFieldname(descriptor, useCase, context);
                                                    if (Objects.nonNull(fieldname)) {
                                                        final Object value = doc.getContextualizedValue(descriptor, context);
                                                        final Object caseValue = SolrUtils.FieldValue.getFieldCaseValue(value, descriptor, useCase);
                                                        if(Objects.nonNull(caseValue)) {
                                                            document.addField(
                                                                fieldname,
                                                                toSolrJType(caseValue)
                                                                //,descriptor.getBoost() TODO should we have index time boost?
                                                            );
                                                        }
                                                    }
                                                }
                                        )));

        //add subdocuments
        if (doc.hasChildren()) {
            doc.getChildren().forEach(childDocument ->
                            document.addChildDocument(createInputDocument(childDocument))
            );
        }

        document.addField(ID, doc.getId());
        document.addField(TYPE, doc.getType());

        return document;
    }

    private Object toSolrJType(Object value) {
        if(value!=null) {
            if(Object[].class.isAssignableFrom(value.getClass())){
                return toSolrJType(Arrays.asList((Object[])value));
            }
            if(Collection.class.isAssignableFrom(value.getClass())){
                return((Collection)value).stream()
                        .map(o -> toSolrJType(o))
                        .collect(Collectors.toList());
            }
            if(value instanceof ZonedDateTime) {
                return Date.from(((ZonedDateTime) value).toInstant());
            }
            if(value instanceof LatLng) {
                return value.toString();
            }
            if(value instanceof Date) {
                //noinspection RedundantCast
                return ((Date) value);
            }
        }
        return value;
    }

    @Override
    public void commit(boolean optimize) {
        try {
            solrClientLogger.debug(">>> commit()");
            this.solrClient.commit();
            if(optimize) {
                solrClientLogger.debug(">>> optimize()");
                this.solrClient.optimize();
            }
        } catch (SolrServerException | IOException e) {
            log.error("Cannot commit", e);
            throw new SearchServerException("Cannot commit", e);
        }
    }

    @Override
    public <T> BeanSearchResult<T> execute(FulltextSearch search, Class<T> c) {
        final DocumentFactory factory = AnnotationUtil.createDocumentFactory(c);

        final SearchResult docResult = this.execute(search, factory);

        return docResult.toPojoResult(docResult, c);
    }

    @Override
    public DeleteResult delete(Document doc) {
        return this.deleteWithin(doc, -1);
    }

    @Override
    public DeleteResult deleteWithin(Document doc, int withinMs) {
        try {
            long qTime = 0;
            long elapsedTime = 0;
            solrClientLogger.debug(">>> delete({})", doc.getId());
            final UpdateResponse deleteResponse = withinMs < 0 ? solrClient.deleteById(doc.getId()) : solrClient.deleteById(doc.getId(), withinMs);
            qTime = deleteResponse.getQTime();
            elapsedTime = deleteResponse.getElapsedTime();
            //Deleting nested documents
            final UpdateResponse deleteNestedResponse = withinMs < 0 ? solrClient.deleteByQuery("_root_:" + doc.getId()) : solrClient.deleteByQuery("_root_:" + doc.getId(), withinMs);
            qTime += deleteNestedResponse.getQTime();
            elapsedTime += deleteNestedResponse.getElapsedTime();

            return new DeleteResult(qTime).setElapsedTime(elapsedTime);
        } catch (SolrServerException | IOException e) {
            log.error("Cannot delete document {}", doc.getId() , e);
            throw new SearchServerException("Cannot delete document", e);
        }
    }

    @Override
    public SearchResult execute(FulltextSearch search, DocumentFactory factory) {
        final SolrQuery query = buildSolrQuery(search, factory);
        //query
        try {
            solrClientLogger.debug(">>> query({})", query.toString());
            final QueryResponse response = solrClient.query(query, SolrRequest.METHOD.POST);
            if(response!=null){

                final Map<String,Integer> childCounts = SolrUtils.getChildCounts(response);

                final List<Document> documents = SolrUtils.Result.buildResultList(response.getResults(), childCounts, factory, search.getSearchContext());
                final FacetResults facetResults = SolrUtils.Result.buildFacetResult(response, factory, search.getChildrenFactory(), search.getFacets(),search.getSearchContext());

                switch(search.getResultSet().getType()) {
                    case page:{
                        return new PageResult(response.getResults().getNumFound(), response.getQTime(), documents, search, facetResults, this, factory).setElapsedTime(response.getElapsedTime());
                    }
                    case slice: {
                        return new SliceResult(response.getResults().getNumFound(), response.getQTime(), documents, search, facetResults, this, factory).setElapsedTime(response.getElapsedTime());
                    }
                    default:
                        return new PageResult(response.getResults().getNumFound(), response.getQTime(), documents, search, facetResults, this, factory).setElapsedTime(response.getElapsedTime());
                }
            }else {
                throw new SolrServerException("Null result from SolrClient");
            }

        } catch (SolrServerException | IOException e) {
            throw new SearchServerException("Cannot issue query", e);
        }
    }

    @Override
    public String getRawQuery(FulltextSearch search, DocumentFactory factory) {
        final SolrQuery query = buildSolrQuery(search, factory);
        return query.toString();
    }

    @Override
    public <T> String getRawQuery(FulltextSearch search, Class<T> c) {
        final DocumentFactory factory = AnnotationUtil.createDocumentFactory(c);
        final SolrQuery query = buildSolrQuery(search, factory);
        return query.toString();
    }

    protected SolrQuery buildSolrQuery(FulltextSearch search, DocumentFactory factory) {
        //build query
        final SolrQuery query = new SolrQuery();
        final String searchContext = search.getSearchContext();

        if(search.getTimeZone() != null) {
            query.set(CommonParams.TZ,search.getTimeZone());
        }

        // fulltext search
        query.set(CommonParams.Q, search.getSearchString());

        if(SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_SHOW_SCORE, true)) {
            query.set(CommonParams.FL, "*,score");
        } else {
            query.set(CommonParams.FL, "*");
        }

        if(search.getGeoDistance() != null) {
            final FieldDescriptor descriptor = factory.getField(search.getGeoDistance().getFieldName());
            if (Objects.nonNull(descriptor)) {
                query.setParam(CommonParams.FL, query.get(CommonParams.FL) + "," + DISTANCE + ":geodist()");
                query.setParam("pt", search.getGeoDistance().getLocation().toString());
                query.setParam("sfield", getFieldname(descriptor, UseCase.Facet, searchContext));
            }
        }

        Collection<FieldDescriptor<?>> fulltext = factory.listFields().stream().filter(FieldDescriptor::isFullText).collect(Collectors.toList());
        if(!fulltext.isEmpty()) {
            query.setParam(DisMaxParams.QF, SolrUtils.Query.buildQueryFieldString(fulltext, searchContext));
            query.setParam("defType","edismax");

        } else {
            query.setParam(CommonParams.DF, TEXT);
        }

        //filters
        query.add(CommonParams.FQ,"_type_:"+factory.getType());

        if(search.hasFilter()) {
            SolrUtils.Query.buildFilterString(search.getFilter(), factory,search.getChildrenFactory(),query, searchContext, search.getStrict());
        }

        // fulltext search deep search
        if(search.isChildrenSearchEnabled()) {
            //append childCount facet
            search.facet(new Facet.SubdocumentFacet(factory));
            //TODO: move to SolrUtils
            final String parentSearchQuery = "((" + query.get(CommonParams.Q) + ") AND " + TYPE + ":" + factory.getType() + ")";

            final String childrenSearchQuery =
                    search.getChildrenSearches().stream()
                    .map( childrenSearch ->
                            "_query_:\"{!parent which="+ TYPE+":"+factory.getType()+"}(" + TYPE+":"+search.getChildrenFactory().getType()+" AND (" + childrenSearch.getEscapedSearchString()+"))\"")
                    .collect(Collectors.joining( " " + search.getChildrenSearchOperator().name() + " "));

            query.set(CommonParams.Q, String.join(" ",
                    parentSearchQuery,
                    search.getChildrenSearchOperator().name(),
                    childrenSearchQuery));

            search.getChildrenSearches().forEach( childrenSearch -> {

                if(childrenSearch.hasFilter()){

                    //TODO clean up!
                    final String parentFilterQuery =  "(" + String.join(" AND ", query.getFilterQueries()) + ")";
                    final String childrenFilterQuery =
                            new ChildrenFilterSerializer(factory,search.getChildrenFactory(),searchContext, search.getStrict(), true)
                                    .serialize(childrenSearch.getFilter());

                    query.set(CommonParams.FQ,
                            String.join(" ",
                                    parentFilterQuery,
                                    search.getChildrenSearchOperator().name(),
                                    "(" + childrenFilterQuery + ")"));
                }

                    }
            );


        }


        if(search.hasFacet()) {
            query.setFacet(true);

            query.setFacetMinCount(search.getFacetMinCount());
            query.setFacetLimit(search.getFacetLimit());

            //Query facets
            search.getFacets().values().stream()
                    .filter(facet -> Facet.QueryFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.QueryFacet)genericFacet)
                    .forEach(queryFacet ->
                           query.addFacetQuery(StringUtils.join(SolrUtils.Query.buildSolrFacetCustomName(SolrUtils.Query.buildFilterString(queryFacet.getFilter(), factory, search.getChildrenFactory(), searchContext,search.getStrict()), queryFacet)))
                    );
            //Numeric Range facet
            search.getFacets().values().stream()
                    .filter(facet -> Facet.NumericRangeFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.NumericRangeFacet) genericFacet)
                    .forEach(numericRangeFacet -> {
                        final UseCase useCase = UseCase.valueOf(numericRangeFacet.getScope().name());
                        final String fieldName =
                                getFieldname(numericRangeFacet.getFieldDescriptor(), useCase, searchContext);

                        query.add(FacetParams.FACET_RANGE,SolrUtils.Query.buildSolrFacetCustomName(fieldName, numericRangeFacet));
                        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                                FacetParams.FACET_RANGE_START),
                                numericRangeFacet.getStart().toString());
                        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                                FacetParams.FACET_RANGE_END),
                                numericRangeFacet.getEnd().toString());
                        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                                FacetParams.FACET_RANGE_GAP),
                                numericRangeFacet.getGap().toString());
                            /*query.addNumericRangeFacet(
                                    SolrUtils.Query.buildSolrFacetCustomName(fieldName, numericRangeFacet.getName()),
                                    numericRangeFacet.getStart(),
                                    numericRangeFacet.getEnd(),
                                    numericRangeFacet.getGap());*/});

            //Interval Range facet
            search.getFacets().values().stream()
                    .filter(facet -> Facet.IntervalFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.IntervalFacet) genericFacet)
                    .forEach(intervalFacet -> {
                        final UseCase useCase = UseCase.valueOf(intervalFacet.getScope().name());
                        final String fieldName = getFieldname(intervalFacet.getFieldDescriptor(), useCase, searchContext);

                        query.add(FacetParams.FACET_INTERVAL, SolrUtils.Query.buildSolrFacetKey(intervalFacet.getFacetName()) + fieldName);

                        for(Object o : intervalFacet.getIntervals()) {
                            Interval i = (Interval) o; //TODO why is this necessary?
                            query.add(String.format("f.%s.%s", fieldName, FacetParams.FACET_INTERVAL_SET),
                                    String.format("%s%s%s,%s%s",
                                            SolrUtils.Query.buildSolrFacetKey(i.getName()),
                                            i.includesStart() ? "[" : "(",
                                            i.getStart() == null? SOLR_WILDCARD : SolrUtils.Query.buildSolrQueryValue(i.getStart()),
                                            i.getEnd() == null? SOLR_WILDCARD : SolrUtils.Query.buildSolrQueryValue(i.getEnd()),
                                            i.includesEnd() ? "]" : ")")
                                    );
                        }
                    });

            //Date Range facet
            search.getFacets().values().stream()
                    .filter(facet -> Facet.DateRangeFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.DateRangeFacet)genericFacet)
                    .forEach(dateRangeFacet ->
                                    generateDateRangeQuery(dateRangeFacet, query, searchContext)
                    );
            //stats
            search.getFacets().values().stream()
                    .filter(facet -> Facet.StatsFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.StatsFacet)genericFacet)
                    .forEach(statsFacet -> {

                        final UseCase useCase = UseCase.valueOf(statsFacet.getScope().name());
                        String fieldName = getFieldname(statsFacet.getField(), useCase, searchContext);

                        query.add(StatsParams.STATS, "true");
                        query.add(StatsParams.STATS_FIELD, SolrUtils.Query.buildSolrStatsQuery(fieldName, statsFacet));
                    });
            //pivot facet
            search.getFacets().values().stream()
                    .filter(facet -> Facet.PivotFacet.class.isAssignableFrom(facet.getClass()))
                    .map(genericFacet -> (Facet.PivotFacet)genericFacet)
                    .forEach(pivotFacet -> {
                        String[] fieldNames=
                                pivotFacet.getFieldDescriptors().stream()
                                        .map(fieldDescriptor -> getFieldname(fieldDescriptor, UseCase.Facet, searchContext))
                                        .toArray(String[]::new);

                        query.add(FacetParams.FACET_PIVOT,SolrUtils.Query.buildSolrPivotSubFacetName(pivotFacet.getFacetName(),fieldNames));
                    });

            //facet fields
            final HashMap<String, Object> strings = SolrUtils.Query.buildJsonTermFacet(search.getFacets(), search.getFacetLimit(), factory, search.getChildrenFactory(), searchContext);

            query.add("json.facet", strings.toString().replaceAll("=",":"));
            //facet Subdocument count
            final String subdocumentFacetString = SolrUtils.Query.buildSubdocumentFacet(search, factory, searchContext);
            if(Objects.nonNull(subdocumentFacetString)) {
                query.add("json.facet", subdocumentFacetString);
            }
        }

        // sorting
        if(search.hasSorting()) {
            final String sortString = SolrUtils.Query.buildSortString(search, search.getSorting(), factory);

            query.set(CommonParams.SORT, sortString);
        }

        //boost functions
        //TODO this is a mess
        if(search.hasSorting()) {
            query.set(DisMaxParams.BF, SolrUtils.Query.buildBoostFunction(search.getSorting(), searchContext));
        }

        // paging
        switch(search.getResultSet().getType()) {
            case page:{
                final Page resultSet = (Page) search.getResultSet();
                query.setStart(resultSet.getOffset());
                query.setRows(resultSet.getPagesize());
                break;
            }
            case slice: {
                final Slice resultSet = (Slice) search.getResultSet();
                query.setStart(resultSet.getOffset());
                query.setRows(resultSet.getSliceSize());
                break;
            }
        }
        return query;
    }

    private void generateDateRangeQuery(Facet.DateRangeFacet dateRangeFacet, SolrQuery query, String searchContext) {

        final UseCase useCase = UseCase.valueOf(dateRangeFacet.getScope().name());
        final String fieldName = getFieldname(dateRangeFacet.getFieldDescriptor(), useCase, searchContext);

        query.add(FacetParams.FACET_RANGE,SolrUtils.Query.buildSolrFacetCustomName(fieldName, dateRangeFacet));

        final Object startDate = dateRangeFacet.getStart();
        final Object endDate = dateRangeFacet.getEnd();

        String startString;
        String endString;

        if (dateRangeFacet instanceof Facet.DateRangeFacet.UtilDateRangeFacet){
            final Instant startInstant = ((Date) startDate).toInstant();
            startString =  DateUtil.getThreadLocalDateFormat().format(Date.from(startInstant));

            final Instant endInstant = ((Date) endDate).toInstant();
            endString = DateUtil.getThreadLocalDateFormat().format(Date.from(endInstant));

        } else if (dateRangeFacet instanceof Facet.DateRangeFacet.ZoneDateRangeFacet){
            final Instant startInstant = ((ZonedDateTime) startDate).toInstant();
            startString =  DateUtil.getThreadLocalDateFormat().format(Date.from(startInstant));

            final Instant  endInstant = ((ZonedDateTime) endDate).toInstant();
            endString = DateUtil.getThreadLocalDateFormat().format(Date.from(endInstant));
        } else {
            startString =  dateRangeFacet.getStart().toString();
            endString = dateRangeFacet.getEnd().toString();
        }

        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                FacetParams.FACET_RANGE_START),
                startString);
        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                FacetParams.FACET_RANGE_END),
                endString);
        query.add(String.format(Locale.ROOT, "f.%s.%s", fieldName,
                FacetParams.FACET_RANGE_GAP),
                SolrUtils.Query.buildSolrTimeGap(dateRangeFacet.getGap()));
    }

    @Override
    public boolean execute(Update update,DocumentFactory factory) {

        //Check if document is updatable and all its fields are stored.
        final boolean isUpdatable = factory.isUpdatable() && factory.getFields().values().stream()
                                        .allMatch( descriptor -> descriptor.isUpdate());
        if (isUpdatable) {

            //Creates an atomic update solr document
            final SolrInputDocument sdoc = getSolrUpdateDocument(update, factory.getType());

            try {

                if (solrClientLogger.isTraceEnabled()) {
                    solrClientLogger.debug(">>> add({}): {}", update.getId(), sdoc);
                } else {
                    solrClientLogger.debug(">>> add({})", update.getId());
                }

                SolrInputDocument finalDoc = sdoc;

                //Get the original document
                log.debug("Atomic Update - Get version of original document [{}].",update.getId());
                final SolrDocument updatedDoc = solrClient.getById(update.getId());

                //Setting the document version for optimistic concurrency
                final Object version = updatedDoc.getFieldValue("_version_");
                if (Objects.nonNull(version)) {
                    finalDoc.setField("_version_", version);
                } else {
                    log.warn("Error updating document [{}]: " +
                            "Atomic updates in nested documents are not supported by Solr", updatedDoc.get(ID));

                    return false;
                }

                //Get the nested docs of the document if existing
                log.debug("Atomic Update - Get nested documents of [{}].",update.getId());
                final NamedList<Object> paramList = new NamedList<>();
                paramList.add(CommonParams.Q, "!( _id_:"+ update.getId()+")&(_root_:"+ update.getId()+")");
                final QueryResponse query = solrClient.query(SolrParams.toSolrParams(paramList), SolrRequest.METHOD.POST);

                //if the document has nested docs solr does not support atomic updates
                if (CollectionUtils.isNotEmpty(query.getResults())) {
                    log.debug("Update document [{}]: doc has {} nested documents, changing from partial update to full index.",
                            finalDoc.getFieldValue(SolrUtils.Fieldname.ID), query.getResults().size());
                    //Get the list of nested documents
                    final List<SolrInputDocument> childDocs = query.getResults().stream()
                            .map(nestedDoc -> ClientUtils.toSolrInputDocument(nestedDoc))
                            .collect(Collectors.toList());

                    finalDoc = this.getUpdatedSolrDocument(sdoc, updatedDoc, childDocs);
                }

                try {
                    log.debug("Atomic Update - Updating document [{}]: current version [{}]", finalDoc.getFieldValue(SolrUtils.Fieldname.ID), version);
                    final UpdateResponse response = solrClient.add(finalDoc);
                    log.debug("Atomic Update - Solr update time: query time [{}] - elapsed time [{}]", response.getQTime(), response.getQTime());
                    return true;
                } catch (HttpSolrClient.RemoteSolrException e) {
                    log.warn("Error updating document [{}]: [{}]", finalDoc.getFieldValue(ID),e.getMessage(), e);
                    return false;
                }

            } catch (SolrServerException | IOException e) {
                log.error("Unable to perform solr partial update on document with id [{}]", update.getId(), e);
                throw new SearchServerException("Can not execute solr partial update.", e);
            }
        } else {
            Exception e = new SearchServerException("It is not safe to execute solr partial update: Document contains non stored fields");
            log.error("Unable to perform solr partial update on document with id [{}]", update.getId(), e);
            throw new RuntimeException("Can not execute solr partial update.", e);
        }
    }

    private SolrInputDocument getSolrUpdateDocument(Update update, String type) {

        final SolrInputDocument sdoc = new SolrInputDocument();
        sdoc.addField(ID, update.getId());
        sdoc.addField(TYPE, type);

        log.debug("Atomic Update - Mapping the Vind update operations to a solr document with ID [{}].", update.getId());
        final HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> updateOptions = update.getOptions();

        log.debug("Atomic Update - Updating {} fields.", updateOptions.keySet().size());
        updateOptions.keySet()
                .forEach(fieldDescriptor -> {
                    log.debug("Atomic Update - Updating {} different contexts for field [{}].", updateOptions.get(fieldDescriptor).keySet().size(), fieldDescriptor);
                    updateOptions.get(fieldDescriptor).keySet()
                        .stream().forEach(context ->
                            Stream.of(UseCase.values()).forEach(useCase -> {
                                //NOTE: Backwards compatibility
                                final String updateContext = Objects.isNull(context)? update.getUpdateContext() : context;
                                final String fieldName = getFieldname(fieldDescriptor, useCase, updateContext);
                                if (fieldName != null) {
                                    final Map<String, Object> fieldModifiers = new HashMap<>();
                                    updateOptions.get(fieldDescriptor).get(context).stream().forEach(entry -> {
                                        UpdateOperations opType = entry.getType();
                                        if(fieldName.startsWith("dynamic_single_") && useCase.equals(UseCase.Sort) && opType.equals(UpdateOperations.add)) {
                                            opType = set;
                                        }
                                        fieldModifiers.put(opType.name(),
                                                toSolrJType(SolrUtils.FieldValue.getFieldCaseValue(entry.getValue(), fieldDescriptor, useCase)));

                                    });
                                    sdoc.addField(fieldName, fieldModifiers);
                                }
                        })
                    );
                    }
                );
        return sdoc;
    }

    private SolrInputDocument getUpdatedSolrDocument(SolrInputDocument sdoc, SolrDocument updatedDoc, List<SolrInputDocument> nestedDocs) {

        //TODO:find a better way - non deprecated way
        //Create an input document from the original doc to be updated
        final SolrInputDocument inputDoc = ClientUtils.toSolrInputDocument(updatedDoc);

        log.debug("Atomic Update - Manually update Document [{}].", sdoc.getField(ID).getValue());

        //Add nested documents to the doc
        inputDoc.addChildDocuments(nestedDocs);

        //TODO: think about a cleaner solution
        //Update the original document
        sdoc.getFieldNames().stream()
                .filter(fn -> !fn.equals(ID) && !fn.equals(TYPE) && !fn.equals("_version_") )//TODO: Add all the special fields or do the oposite check, whether it fits a dynamic Vind field
                .forEach( fn -> {
                    final ArrayList fieldOp = (ArrayList) sdoc.getFieldValues(fn);
                    fieldOp.stream()
                            .forEach( op -> {
                                final Set<String> keys = ((HashMap<String, String>) op).keySet();
                                keys.stream()
                                        .forEach(k -> {
                                            switch (UpdateOperations.valueOf(k)) {
                                                case set:
                                                    inputDoc.setField(fn, ((HashMap<String, String>) op).get(k) );
                                                    break;
                                                case add:
                                                    inputDoc.addField(fn, ((HashMap<String, String>) op).get(k) );
                                                    break;
                                                case inc:
                                                    final Number fieldValue;
                                                    try {
                                                        fieldValue = NumberFormat.getInstance().parse((String)inputDoc.getFieldValue(fn));

                                                    } catch (ParseException e) {
                                                        throw new RuntimeException();
                                                    }
                                                    inputDoc.setField(fn,String.valueOf(fieldValue.floatValue()+1));
                                                    break;
                                                case remove:
                                                    inputDoc.removeField(fn);
                                                    break;
                                                case removeregex:
                                                    final String fieldStringValue = (String)inputDoc.getFieldValue(fn);
                                                    final String regex = ((HashMap<String, String>) op).get(k);
                                                    if (regex.matches(fieldStringValue)) {
                                                        inputDoc.removeField(fn);
                                                    }
                                                    break;
                                            }
                                        });
                                }
                            );

                    }
                );

        return inputDoc;
    }

    @Override
    public DeleteResult execute(Delete delete, DocumentFactory factory) {
        String query = SolrUtils.Query.buildFilterString(delete.getQuery(), factory, delete.getUpdateContext(),true);
        try {
            solrClientLogger.debug(">>> delete query({})", query);
            //Finding the ID of the documents to delete
            final SolrQuery solrQuery = new SolrQuery();
            solrQuery.setParam(CommonParams.Q, "*:*");
            solrQuery.setParam(CommonParams.FQ,query.trim().replaceAll("^\\+","").split("\\+"));
            final QueryResponse response = solrClient.query(solrQuery, SolrRequest.METHOD.POST);
            long qTime = 0;
            long elapsedTime = 0;
            if(Objects.nonNull(response) && CollectionUtils.isNotEmpty(response.getResults())){
                final List<String> idList = response.getResults().stream().map(doc -> (String) doc.get(ID)).collect(Collectors.toList());
                final UpdateResponse deleteResponse = solrClient.deleteById(idList);
                qTime = deleteResponse.getQTime();
                elapsedTime = deleteResponse.getElapsedTime();
                //Deleting nested documents
                final UpdateResponse deleteNestedResponse = solrClient.deleteByQuery("_root_:(" + StringUtils.join(idList, " OR ") + ")");

                qTime += deleteNestedResponse.getQTime();
                elapsedTime += deleteNestedResponse.getElapsedTime();
            }

            return new DeleteResult(qTime).setElapsedTime(elapsedTime);
        } catch (SolrServerException | IOException e) {
            log.error("Cannot delete with query {}", query, e);
            throw new SearchServerException("Cannot delete with query", e);
        }
    }

    @Override
    public <T> SuggestionResult execute(ExecutableSuggestionSearch search, Class<T> c) {
        DocumentFactory documentFactory = AnnotationUtil.createDocumentFactory(c);
        return this.execute(search, documentFactory);
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets) {
        return execute(search,assets,null);
    }

    @Override
    public SuggestionResult execute(ExecutableSuggestionSearch search, DocumentFactory assets,DocumentFactory childFactory) {
        SolrQuery query = buildSolrQuery(search, assets, childFactory);

        try {
            log.debug(">>> query({})", query.toString());
            QueryResponse response = solrClient.query(query, SolrRequest.METHOD.POST);
            if(response!=null){
                return SolrUtils.Result.buildSuggestionResult(response, assets, childFactory, search.getSearchContext());
            }else {
                log.error("Null result from SolrClient");
                throw new SolrServerException("Null result from SolrClient");
            }

        } catch (SolrServerException | IOException e) {
            log.error("Cannot execute suggestion query");
            throw new SearchServerException("Cannot execute suggestion query", e);
        }
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory) {
        final SolrQuery query = buildSolrQuery(search, factory, null);
        return query.toString();
    }

    @Override
    public String getRawQuery(ExecutableSuggestionSearch search, DocumentFactory factory, DocumentFactory childFactory) {
        final SolrQuery query = buildSolrQuery(search, factory, childFactory);
        return query.toString();
    }

    @Override
    public <T> String getRawQuery(ExecutableSuggestionSearch search, Class<T> c) {
        final DocumentFactory factory = AnnotationUtil.createDocumentFactory(c);
        final SolrQuery query = buildSolrQuery(search, factory, null);
        return query.toString();
    }

    protected SolrQuery buildSolrQuery(ExecutableSuggestionSearch search, DocumentFactory assets, DocumentFactory childFactory) {
        final String searchContext = search.getSearchContext();

        final SolrQuery query = new SolrQuery();
        query.setRequestHandler("/suggester");

        if(search.isStringSuggestion()) {
            StringSuggestionSearch s = (StringSuggestionSearch) search;

            query.setParam("suggestion.field", s.getSuggestionFields()
                    .stream()
                    .map(name -> {
                        if(Objects.nonNull(childFactory)) {
                            final FieldDescriptor<?> field = Objects.nonNull(assets.getField(name))?
                                    assets.getField(name):
                                    childFactory.getField(name);
                            if(Objects.isNull(field)) {
                                log.warn("No field descriptor found for field name {} in factories: {}, {}", name,assets.getType(), childFactory.getType());
                            }
                            return getFieldname(field, UseCase.Suggest, searchContext);
                        } else {
                            if(Objects.isNull(assets.getField(name))) {
                                log.warn("No field descriptor found for field name {} in factory: {}", name,assets.getType());
                            }
                            return getFieldname(assets.getField(name), UseCase.Suggest, searchContext);
                            }
                    })
                    .filter(Objects::nonNull)
                    .toArray(String[]::new));
        } else {
            DescriptorSuggestionSearch s = (DescriptorSuggestionSearch) search;

            query.setParam("suggestion.field", s.getSuggestionFields()
                    .stream()
                    .map(descriptor -> getFieldname(descriptor, UseCase.Suggest, searchContext))
                    .filter(Objects::nonNull)
                    .toArray(String[]::new));
        }

        query.setParam("q", search.getInput());
        query.setParam("suggestion.df", SUGGESTION_DF_FIELD);//TODO: somehow this is still needed here, it should by configuration

        query.setParam("suggestion.limit", String.valueOf(search.getLimit()));


        String parentTypeFilter = "_type_:" + assets.getType();

        if(Objects.nonNull(childFactory)) {
            parentTypeFilter ="("+parentTypeFilter+" OR _type_:" + childFactory.getType() + ")";
        }

        query.add(CommonParams.FQ, parentTypeFilter);

        //filters
        if(search.hasFilter()) {
            SolrUtils.Query.buildFilterString(search.getFilter(), assets,childFactory,query, searchContext, false);
        }

        // suggestion deep search
        if(Objects.nonNull(childFactory)) {
            if(search.hasFilter()){

                //TODO clean up!
                final String parentFilterQuery =  "(" + String.join(" AND ", query.getFilterQueries()) + ")";
                final String childrenFilterQuery =
                        new ChildrenFilterSerializer(assets, childFactory, searchContext, false, true)
                                .serialize(search.getFilter());

                final String childrenBJQ = "{!child of=\"_type_:"+assets.getType()+"\" v="+"$childrenFilterQuery"+"}";
                query.set("childrenFilterQuery", childrenFilterQuery);
                query.set(CommonParams.FQ, String.join(" OR ", parentFilterQuery, childrenBJQ));
            }

        }
        return query;
    }

    @Override
    public <T> BeanGetResult<T> execute(RealTimeGet search, Class<T> c) {
        DocumentFactory documentFactory = AnnotationUtil.createDocumentFactory(c);
        final GetResult result = this.execute(search, documentFactory);
        return result.toPojoResult(result,c);
    }

    @Override
    public GetResult execute(RealTimeGet search, DocumentFactory assets) {
        SolrQuery query = buildSolrQuery(search, assets);

        try {
            log.debug(">>> query({})", query.toString());
            QueryResponse response = solrClient.query(query, SolrRequest.METHOD.POST);
            if(response!=null){
                return SolrUtils.Result.buildRealTimeGetResult(response, search, assets);
            }else {
                log.error("Null result from SolrClient");
                throw new SolrServerException("Null result from SolrClient");
            }

        } catch (SolrServerException | IOException e) {
            log.error("Cannot execute realTime get query");
            throw new SearchServerException("Cannot execute realTime get query", e);
        }
    }

    @Override
    public void clearIndex() {
        try {
            solrClientLogger.debug(">>> clear complete index");
            solrClient.deleteByQuery("*:*");
        } catch (SolrServerException | IOException e) {
            log.error("Cannot clear index", e);
            throw new SearchServerException("Cannot clear index", e);
        }
    }

    protected SolrQuery buildSolrQuery(RealTimeGet search, DocumentFactory assets) {
        SolrQuery query = new SolrQuery();
        query.setRequestHandler("/get");

        search.getValues().forEach(v -> query.add("id" , v.toString()));
        return query;
    }

    @Override
    public void close() {
        if (solrClient != null) try {
            solrClient.close();
        } catch (IOException e) {
            log.error("Cannot close search server", e);
            throw new SearchServerException("Cannot close search server", e);
        }
    }

    protected static SolrServerProvider getSolrServerProvider() {

        String providerClassName = SearchConfiguration.get(SearchConfiguration.SERVER_PROVIDER, null);

        //Backwards compatibility needed
        final String solrProviderClassName = SearchConfiguration.get(SearchConfiguration.SERVER_SOLR_PROVIDER, null);
        if (providerClassName == null && solrProviderClassName != null) {
            providerClassName = solrProviderClassName;
        }
        final ServiceLoader<SolrServerProvider> loader = ServiceLoader.load(SolrServerProvider.class);
        final Iterator<SolrServerProvider> it = loader.iterator();

        SolrServerProvider serverProvider = null;
        if(providerClassName == null) {
            if (!it.hasNext()) {
                log.error("No SolrServerProvider in classpath");
                throw new RuntimeException("No SolrServerProvider in classpath");
            } else {
                serverProvider = it.next();
            }
            if (it.hasNext()) {
                log.warn("Multiple bindings for SolrServerProvider found: {}", loader.iterator());
            }
        } else {
            try {
                final Class<?> providerClass = Class.forName(providerClassName);

                while(it.hasNext()) {
                    final SolrServerProvider p = it.next();
                    if(providerClass.isAssignableFrom(p.getClass())) {
                        serverProvider = p;
                        break;
                    }
                }

            } catch (ClassNotFoundException e) {
                log.warn("Specified class {} is not in classpath",providerClassName, e);
                //throw new RuntimeException("Specified class " + providerClassName + " is not in classpath");
            }
            log.info("No server provider of type class {} found in classpath for server {}", providerClassName, SolrSearchServer.class.getCanonicalName());
            //if(serverProvider == null) throw new RuntimeException("No server provider found for class " + providerClassName);
        }

        return serverProvider;
    }

    @Override
    public Class getServiceProviderClass() {
        return serviceProviderClass!=null? serviceProviderClass.getClass() : null;
    }
}
