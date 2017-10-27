package com.rbmhtechnology.vind.solr.backend;

import com.google.common.io.Resources;
import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.ServiceProvider;
import com.rbmhtechnology.vind.api.query.*;
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
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.solr.backend.SolrUtils.Query.buildFilterString;

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
    public void index(Document ... docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(docs.length > 0, "Should be at least one document to index.");
        for(Document doc: docs) {
            indexSingleDocument(doc);
        }
    }

    @Override
    public void index(List<Document> docs) {
        Asserts.notNull(docs,"Document to index should not be null.");
        Asserts.check(docs.size() > 0, "Should be at least one document to index.");

        indexMultipleDocuments(docs);
    }

    private void indexSingleDocument(Document doc) {
        final SolrInputDocument document = createInputDocument(doc);
        try {
            if (solrClientLogger.isTraceEnabled()) {
                solrClientLogger.debug(">>> add({}): {}", doc.getId(), ClientUtils.toXML(document));
            } else {
                solrClientLogger.debug(">>> add({})", doc.getId());
            }
            this.solrClient.add(document);
        } catch (SolrServerException | IOException e) {
            log.error("Cannot index document {}", document.getField(SolrUtils.Fieldname.ID) , e);
            throw new SearchServerException("Cannot index document", e);
        }
    }

    private void indexMultipleDocuments(List<Document> docs) {
        final List<SolrInputDocument> solrDocs = docs.parallelStream()
                .map(doc -> createInputDocument(doc))
                .collect(Collectors.toList());
        try {
            if (solrClientLogger.isTraceEnabled()) {
                solrClientLogger.debug(">>> add({})", solrDocs);
            } else {
                solrClientLogger.debug(">>> add({})", solrDocs);
            }
            this.solrClient.add(solrDocs);
        } catch (SolrServerException | IOException e) {
            log.error("Cannot index documents {}", solrDocs, e);
            throw new SearchServerException("Cannot index documents", e);
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
                                Stream.of(SolrUtils.Fieldname.UseCase.values())
                                        .forEach(useCase -> {
                                                    final String fieldname = SolrUtils.Fieldname.getFieldname(descriptor, useCase, context);
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

        document.addField(SolrUtils.Fieldname.ID, doc.getId());
        document.addField(SolrUtils.Fieldname.TYPE, doc.getType());

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
    public void delete(Document doc) {
        try {
            solrClientLogger.debug(">>> delete({})", doc.getId());
            solrClient.deleteById(doc.getId());
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
                final FacetResults facetResults = SolrUtils.Result.buildFacetResult(response, factory,search.getFacets(),search.getSearchContext());

                switch(search.getResultSet().getType()) {
                    case page:{
                        return new PageResult(response.getResults().getNumFound(), documents, search, facetResults, this, factory);
                    }
                    case slice: {
                        return new SliceResult(response.getResults().getNumFound(), documents, search, facetResults, this, factory);
                    }
                    default:
                        return new PageResult(response.getResults().getNumFound(), documents, search, facetResults, this, factory);
                }
            }else {
                throw new SolrServerException("Null result from SolrClient");
            }

        } catch (SolrServerException | IOException e) {
            throw new SearchServerException("Cannot issue query", e);
        }
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
                query.setParam(CommonParams.FL, query.get(CommonParams.FL) + "," + SolrUtils.Fieldname.DISTANCE + ":geodist()");
                query.setParam("pt", search.getGeoDistance().getLocation().toString());
                query.setParam("sfield", SolrUtils.Fieldname.getFieldname(descriptor, SolrUtils.Fieldname.UseCase.Facet, searchContext));
            }
        }

        Collection<FieldDescriptor<?>> fulltext = factory.listFields().stream().filter(FieldDescriptor::isFullText).collect(Collectors.toList());
        if(!fulltext.isEmpty()) {
            query.setParam(DisMaxParams.QF, SolrUtils.Query.buildQueryFieldString(fulltext, searchContext));
            query.setParam("defType","edismax");

        } else {
            query.setParam(CommonParams.DF, SolrUtils.Fieldname.TEXT);
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
            final String parentSearchQuery = "(" + query.get(CommonParams.Q) + " AND " + SolrUtils.Fieldname.TYPE + ":" + factory.getType() + ")";

            final String childrenSearchQuery = "_query_:\"{!parent which="+ SolrUtils.Fieldname.TYPE+":"+factory.getType()+"}(" + SolrUtils.Fieldname.TYPE+":"+search.getChildrenFactory().getType()+" AND ("+search.getChildrenSearchString().getEscapedSearchString()+"))\"";

            query.set(CommonParams.Q, String.join(" ",parentSearchQuery, search.getChildrenSearchOperator().name(), childrenSearchQuery));

            if(search.getChildrenSearchString().hasFilter()){

                //TODO clean up!
                final String parentFilterQuery =  "(" + String.join(" AND ", query.getFilterQueries()) + ")";
                final String childrenFilterQuery = search.getChildrenSearchString()
                        .getFilter().accept(new SolrChildrenSerializerVisitor(factory,search.getChildrenFactory(),searchContext, search.getStrict()));

                query.set(CommonParams.FQ, String.join(" ", parentFilterQuery, search.getChildrenSearchOperator().name(), "("+childrenFilterQuery+")"));
            }

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
                        String fieldName = SolrUtils.Fieldname.getFieldname(numericRangeFacet.getFieldDescriptor(), SolrUtils.Fieldname.UseCase.Facet, searchContext);

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

                        String fieldName = SolrUtils.Fieldname.getFieldname(intervalFacet.getFieldDescriptor(), SolrUtils.Fieldname.UseCase.Facet, searchContext);

                        query.add(FacetParams.FACET_INTERVAL, SolrUtils.Query.buildSolrFacetKey(intervalFacet.getName()) + fieldName);

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

                        String fieldName = SolrUtils.Fieldname.getFieldname(statsFacet.getField(), SolrUtils.Fieldname.UseCase.Facet, searchContext);

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
                                        .map(fieldDescriptor -> SolrUtils.Fieldname.getFieldname(fieldDescriptor, SolrUtils.Fieldname.UseCase.Facet, searchContext))
                                        .toArray(String[]::new);

                        query.add(FacetParams.FACET_PIVOT,SolrUtils.Query.buildSolrPivotSubFacetName(pivotFacet.getName(),fieldNames));
                    });

            //facet fields
            query.addFacetField(SolrUtils.Query.buildFacetFieldList(search.getFacets(), factory, searchContext));

            //facet Subdocument count
            final String subdocumentFacetString = SolrUtils.Query.buildSubdocumentFacet(search, factory, searchContext);
            if(Objects.nonNull(subdocumentFacetString)) {
                query.set("json.facet", subdocumentFacetString);
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
        final String fieldName = SolrUtils.Fieldname.getFieldname(dateRangeFacet.getFieldDescriptor(), SolrUtils.Fieldname.UseCase.Facet, searchContext);

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
    public void execute(Update update,DocumentFactory factory) {
        //MBDN-434
        final boolean isUpdatable = factory.isUpdatable() && factory.getFields().values().stream()
                                        .allMatch( descriptor -> descriptor.isUpdate());
        if (isUpdatable) {
            final SolrInputDocument sdoc = new SolrInputDocument();
            sdoc.addField(SolrUtils.Fieldname.ID, update.getId());
            sdoc.addField(SolrUtils.Fieldname.TYPE, factory.getType());

            HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> updateOptions = update.getOptions();
            updateOptions.keySet()
                    .forEach(fieldDescriptor ->
                        Stream.of(SolrUtils.Fieldname.UseCase.values()).forEach(useCase ->
                            updateOptions.get(fieldDescriptor).keySet()
                                .stream().forEach(context -> {
                                    //NOTE: Backwards compatibility
                                    final String updateContext = Objects.isNull(context)? update.getUpdateContext() : context;
                                    final String fieldName = SolrUtils.Fieldname.getFieldname(fieldDescriptor, useCase, updateContext);
                                    if (fieldName != null) {
                                        final Map<String, Object> fieldModifiers = new HashMap<>();
                                        updateOptions.get(fieldDescriptor).get(context).stream().forEach(entry -> {
                                            Update.UpdateOperations opType = entry.getType();
                                            if(fieldName.startsWith("dynamic_single_") && useCase.equals(SolrUtils.Fieldname.UseCase.Sort) && opType.equals(Update.UpdateOperations.add)) {
                                                opType = Update.UpdateOperations.set;
                                            }
                                            fieldModifiers.put(opType.name(),
                                                    toSolrJType(SolrUtils.FieldValue.getFieldCaseValue(entry.getValue(), fieldDescriptor, useCase)));

                                        });
                                        sdoc.addField(fieldName, fieldModifiers);
                                    }
                            })
                        )
                    );
            try {

                if (solrClientLogger.isTraceEnabled()) {
                    solrClientLogger.debug(">>> add({}): {}", update.getId(), sdoc);
                } else {
                    solrClientLogger.debug(">>> add({})", update.getId());
                }
                solrClient.add(sdoc);

                //Get the nested documents for the document to update
                final NamedList<Object> paramList = new NamedList<>();
                paramList.add(CommonParams.Q, "!( _id_:"+ update.getId()+")&(_root_:"+ update.getId()+")");
                final QueryResponse query = solrClient.query(SolrParams.toSolrParams(paramList), SolrRequest.METHOD.POST);

                //Reindex the updated document with its nested document so they are all index together
                if (CollectionUtils.isNotEmpty(query.getResults())) {
                    //get the updated document
                    final SolrDocument updatedDoc = solrClient.getById(update.getId());

                    //TODO:find a better way - non deprecated way
                    final SolrInputDocument inputDoc = ClientUtils.toSolrInputDocument(updatedDoc);
                    inputDoc.addChildDocuments(query.getResults().stream().map(nestedDoc -> ClientUtils.toSolrInputDocument(nestedDoc)).collect(Collectors.toList()));
                    solrClient.add(inputDoc);

                    //MBDN-579: Delete the duplicated document created by solr with old _version_
                    solrClient.deleteByQuery("_version_:" + inputDoc.getField("_version_").getValue() + " AND _id_:" + update.getId());
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

    @Override
    public void execute(Delete delete, DocumentFactory factory) {
        String query = SolrUtils.Query.buildFilterString(delete.getQuery(), factory, delete.getUpdateContext(),true);
        try {
            solrClientLogger.debug(">>> delete query({})", query);
            solrClient.deleteByQuery(query);
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
                            return SolrUtils.Fieldname.getFieldname(field, SolrUtils.Fieldname.UseCase.Suggest, searchContext);
                        } else {
                            return SolrUtils.Fieldname.getFieldname(assets.getField(name), SolrUtils.Fieldname.UseCase.Suggest, searchContext);
                            }
                    })
                    .filter(Objects::nonNull)
                    .toArray(String[]::new));
        } else {
            DescriptorSuggestionSearch s = (DescriptorSuggestionSearch) search;

            query.setParam("suggestion.field", s.getSuggestionFields()
                    .stream()
                    .map(descriptor -> SolrUtils.Fieldname.getFieldname(descriptor, SolrUtils.Fieldname.UseCase.Suggest, searchContext))
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
            new SolrChildrenSerializerVisitor(assets,childFactory,searchContext,false);
        }

        // suggestion deep search
        if(Objects.nonNull(childFactory)) {
            if(search.hasFilter()){

                //TODO clean up!
                final String parentFilterQuery =  "(" + String.join(" AND ", query.getFilterQueries()) + ")";
                final String childrenFilterQuery = search.getFilter()
                        .accept(new SolrChildrenSerializerVisitor(assets,childFactory,searchContext, false));
                final String childrenBJQ = "{!child of=\"_type_:"+assets.getType()+"\" v='"+childrenFilterQuery+"'}";
                query.set(CommonParams.FQ, String.join(" ", parentFilterQuery, "OR", childrenBJQ));
            }

        }
        return query;
    }

    @Override
    public <T> GetResult execute(RealTimeGet search, Class<T> c) {
        DocumentFactory documentFactory = AnnotationUtil.createDocumentFactory(c);
        return this.execute(search,documentFactory);
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
