/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.preprocess;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.util.SimilarityUtils;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.SearchResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.monitoring.report.util.ReportLabels.*;

/**
 * Created on 11.06.18.
 */
public class ReportPreprocessor {
    private static final Logger log = LoggerFactory.getLogger(ReportPreprocessor.class);

    private final Long from;
    protected final Long to;

    private final  ElasticSearchClient elasticClient = new ElasticSearchClient();
    private final String appId;
    private String messageWrapper = "";
    private Long scrollSpan = 1000l;
    private DateTimeFormatter esDateFormater = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private List<JsonElement> environmentFilters = new ArrayList<>();
    private List<String> sessionIds = new ArrayList<>();
    private List<String> systemFieldFilters = new ArrayList<>();

    public ReportPreprocessor(String esHost, String esPort, String esIndex, ZonedDateTime from, ZonedDateTime to, String appId, String messageWrapper) {
        this.from = from.toInstant().toEpochMilli();
        this.to = to.toInstant().toEpochMilli();
        this.messageWrapper = messageWrapper + ".";
        this.appId = appId;

        elasticClient.init(esHost, esPort, esIndex);
    }

    public ReportPreprocessor(String esHost, String esPort, String esIndex, ZonedDateTime from, ZonedDateTime to, String appId) {
        this.from = from.toInstant().toEpochMilli();
        this.to = to.toInstant().toEpochMilli();
        this.appId = appId;

        elasticClient.init(esHost, esPort, esIndex);
    }

    public ReportPreprocessor setMessageWrapper(String messageWrapper) {
        this.messageWrapper = messageWrapper + ".";
        return this;
    }


    public ReportPreprocessor addSystemFilterField(String ... fields) {
        if (Objects.nonNull(fields)) {
            systemFieldFilters.addAll(Arrays.asList(fields));
        }
        return this;
    }
    public void preprocess(){
        preparePreprocessing();
        sessionIds.forEach(this::preprocessSession);
    }

    //Gets the list of sessions to preprocess and figures out the
    // general filters which apply to all queries.
    private void preparePreprocessing() {

        final String query = elasticClient.loadQueryFromFile("prepare",
                this.scrollSpan,
                this.messageWrapper,
                this.appId,
                this.messageWrapper,
                this.messageWrapper,
                this.messageWrapper,
                this.from,
                this.to);

        final JestResult searchResult = elasticClient.getScrollQuery(query);

        String scrollId =
                searchResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();
        final Long totalResults = ((SearchResult) searchResult).getTotal();

        final JsonArray hits = searchResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
        //Get the session Ids for the first result scroll
        final Set<String> sessions =  new HashSet<>();
        sessions.addAll(Streams.stream(hits)
                .map(hit -> hit.getAsJsonObject()
                        .getAsJsonObject("_source")
                        .getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0, (messageWrapper.length()-1))))
                        .getAsJsonObject("session")
                        .get("sessionId").getAsString())
                .collect(Collectors.toList()));

        //Find out filters common to all queries
        final JsonElement initialFilters = hits.get(0).getAsJsonObject()
                                        .getAsJsonObject("_source")
                                        .getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0, (messageWrapper.length()-1))))
                                        .getAsJsonObject("request")
                                        .get("filter");

        final ArrayList<JsonElement> commonFilters =  Lists.newArrayList(extractFilterFields(initialFilters).iterator());
        Streams.stream(hits)
                .map(hit -> extractFilterFields(hit.getAsJsonObject()
                        .getAsJsonObject("_source")
                        .getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0, (messageWrapper.length()-1))))
                        .getAsJsonObject("request")
                        .get("filter")))
                .forEach( hfs -> commonFilters.retainAll(Lists.newArrayList(hfs.iterator())));



        Long start = scrollSpan;
        while (start < totalResults) {

            final JestResult scrollResult =  elasticClient.scrollResults(scrollId);
            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            final JsonArray scrollHits = scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");

            sessions.addAll(Streams.stream(scrollHits)
                    .map(hit -> hit.getAsJsonObject()
                            .getAsJsonObject("_source")
                            .getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0, (messageWrapper.length()-1))))
                            .getAsJsonObject("session")
                            .get("sessionId").getAsString())
                    .collect(Collectors.toList()));

            Streams.stream(scrollHits)
                    .map(hit -> extractFilterFields(hit.getAsJsonObject()
                            .getAsJsonObject("_source")
                            .getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0, (messageWrapper.length()-1))))
                            .getAsJsonObject("request")
                            .get("filter")))
                    .forEach( hfs -> commonFilters.retainAll(Lists.newArrayList(hfs.iterator())));

            start += scrollSpan;
        }

        sessionIds.addAll(sessions);
        environmentFilters.addAll(commonFilters);
    }

    public Boolean preprocessSession(String sessionId) {

        //fetch all the entries for the session

        final String query = elasticClient.loadQueryFromFile("session",
                this.scrollSpan,
                this.messageWrapper,
                this.appId,
                this.messageWrapper,
                sessionId,
                this.messageWrapper);

        final SearchResult searchResult = elasticClient.getScrollQuery(query);

        String scrollId =
                searchResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();
        final Long totalResults = searchResult.getTotal();


        final Set<JsonObject> requests =  new HashSet<>();

        requests.addAll(searchResult.getHits(JsonObject.class).stream()
                .map(es -> es.source.getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0,this.messageWrapper.length()-1))))
                .map( e ->{
                    e.remove("application");
                    e.remove("session");
                    e.remove("response");
                    e.remove("sorting");
                    e.remove("metadata");
                    return e;
                })
                .collect(Collectors.toList()));

        Long start = scrollSpan;
        while (start < totalResults) {

            final JestResult scrollResult =  elasticClient.scrollResults(scrollId);
            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            requests.addAll(Streams.stream(scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits").iterator())
                    .map( e -> e.getAsJsonObject().getAsJsonObject("_source"))
                    .map( jo ->{ if(StringUtils.isNotBlank(this.messageWrapper)){
                                    return jo.getAsJsonObject(String.valueOf(this.messageWrapper.subSequence(0,this.messageWrapper.length()-1)));
                                }
                                return jo;})
                    .map( e ->{
                        e.remove("application");
                        e.remove("session");
                        e.remove("response");
                        e.remove("sorting");
                        e.remove("metadata");
                        return e;
                    })
                    .collect(Collectors.toList()));
            start += scrollSpan;

        }

        //preprocess the entries

        //Sort entries by timeStamp
        final List<JsonObject> sortedRequest = requests.stream()
                //Parse string timestamp to numeric timestamp
                .map( req -> {
                    final DateTimeFormatter f = esDateFormater;
                    final String stringDate = req.get("timeStamp").getAsString();
                    final Long numericDate = ZonedDateTime.parse(stringDate,f).toInstant().toEpochMilli();
                    req.addProperty("timeStamp", numericDate);
                    return req;
                })
                //sort by timestamp
                .sorted(Comparator.comparingLong(jo -> jo.get("timeStamp").getAsLong()))
                .collect(Collectors.toList());

        processSession(sortedRequest);


        return false;
    }

    private void processSession(List<JsonObject> entries) {

        //Exclude suggestion entries
        final List<JsonObject> cleanList = entries.stream()
                .filter( e -> !e.get("type").getAsString().equals("suggestion"))
                .collect(Collectors.toList());

        JsonObject lastQuery = null;
        final List<JsonObject> lastAccesses = new ArrayList<>();
        int searchStep = 1;

        for ( int i = 0 ; i < cleanList.size() ; i++){

            final JsonObject actual = cleanList.get(i);

            //Add pre-processing info
            final JsonObject process = new JsonObject();
            actual.add(SEARCH_PRE_PROCESS_RESULT, process );

            //INTERACTIONS
            if (actual.get("type").getAsString().equals("interaction")) {
                final JsonObject access = actual.getAsJsonObject("request");
                if(access.get("action").getAsString().equals("select")){

                    //Check duplicated accesses
                    if (CollectionUtils.isNotEmpty(lastAccesses)
                            && lastAccesses.contains(access)) {
                        process.addProperty(SEARCH_DUPLICATE,true);
                        process.addProperty(SEARCH_SKIP,true);

                    } else {
                        lastAccesses.add(access);
                        if (Objects.nonNull(lastQuery)) {
                            final JsonObject processInfo = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT);
                            processInfo.addProperty(SEARCH_INTERACTION_SELECT, processInfo.get(SEARCH_INTERACTION_SELECT).getAsLong() + 1);
                        } else {
                            process.addProperty(SEARCH_SKIP,true);
                        }
                    }

                    //TODO:Is the select interaction the end of a user query flow?
                    searchStep = 1;
                    lastQuery = null;
                }
            }

            //FULLTEXT QUERIES
            if (actual.get("type").getAsString().equals("fulltext")) {

                //empty check
                if (isEmptyQuery(actual)) {
                    process.addProperty(SEARCH_SKIP,true);
                    process.addProperty(SEARCH_EMPTY,true);
                    //empty query is the end of an user iteration
                    searchStep = 1;
                    lastQuery = null;
                } else {
                    //Initialize process result json object
                    process.addProperty(SEARCH_INTERACTION_SELECT, 0);
                    process.add(SEARCH_STEPS, new JsonObject());
                    //check if it is a duplicated query
                    if(Objects.nonNull(lastQuery) && lastQuery.getAsJsonObject("request").equals(actual.getAsJsonObject("request"))) {
                        process.addProperty(SEARCH_DUPLICATE, true);
                        process.addProperty(SEARCH_SKIP,true);
                        //TODO: we do not care whether it is a paging action or a sorting action

                    } else {
                        //Clear list of accesses
                        lastAccesses.clear();

                        //Calculate flattened list of filters
                        final JsonElement filters = actual.getAsJsonObject("request").get("filter");
                        final JsonArray flattenedFilters = new JsonArray();
                        flattenedFilters.addAll(extractFilterFields(filters));
                        process.add(SEARCH_FILTERS, flattenedFilters);

                        final JsonArray stepFilters = new JsonArray();
                        stepFilters.addAll(flattenedFilters);
                        if(Objects.nonNull(lastQuery)) {
                            if(isRefinedQuery(actual,lastQuery)) {
                                //Copy previous steps info into this query
                                process.add(SEARCH_STEPS, lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).get(SEARCH_STEPS));

                                final JsonArray lastFilters = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray(SEARCH_FILTERS);
                                //Select new filters for this step
                                Streams.stream(flattenedFilters.iterator())
                                        .filter(lastFilters::contains)
                                        .forEach(f -> stepFilters.remove(f));
                                searchStep ++;
                                lastQuery = actual;
                            } else {
                                searchStep = 1;
                                lastQuery = actual;
                            }
                        } else {
                            searchStep = 1;
                            lastQuery = actual;

                        }

                        process.getAsJsonObject(SEARCH_STEPS).add(String.valueOf(searchStep), stepFilters);
                        process.addProperty(SEARCH_STEP, searchStep);
                        process.addProperty(SEARCH_STRING, actual.getAsJsonObject("request").get("query").getAsString());
                    }

                }


            }
        }

    }

    private Boolean isEmptyQuery (JsonObject query) {

        final String queryText = query.getAsJsonObject("request").get("query").getAsString();
        if (queryText.equals("*")) {
            final JsonArray queryFilters = extractFilterFields(query.getAsJsonObject("request").get("filter"));
            if (queryFilters.size() == 0) {
                return true;
            }
        }
        return false;
    }


    private Boolean isRefinedQuery (JsonObject query, JsonObject lastQuery) {

        final String oldQueryText = lastQuery.getAsJsonObject("request").get("query").getAsString();
        final String actualQueryText = query.getAsJsonObject("request").get("query").getAsString();

        if(isSimilarTextQuery(actualQueryText, oldQueryText)) {
            final JsonArray previousFilters = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray(SEARCH_FILTERS);
            final JsonArray actualFilters = query.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray(SEARCH_FILTERS);
            final Long intersectionCount = Streams.stream(actualFilters.iterator())
                    .filter(previousFilters::contains)
                    .count();
            if ((previousFilters.size() - intersectionCount) <= 1) {
                return true;
            }

        }
        return false;
    }

    private Boolean isSimilarTextQuery (String text, String lastText) {
        if(text.equals(lastText)) {
            return true;
        } else  if (text.contains(lastText)) {
            return true;
        } else if(SimilarityUtils.levenshteinDistance(text, lastText) <= 2) {
            return true;
        }
        return false;
    }

    private JsonArray extractFilterFields(JsonElement filters) {
        final JsonArray fs = new JsonArray();

        //Single filter object or a list of filters
        if (filters.isJsonObject()) {
            fs.add(filters.getAsJsonObject());
        } else {
            fs.addAll(filters.getAsJsonArray());
        }

        return Streams.stream(fs.iterator())
                .map(JsonElement::getAsJsonObject)
                .flatMap( jo -> {
                    if (jo.has("delegates")){
                        return Streams.stream(extractFilterFields(jo.get("delegates")).iterator());
                    } else {
                        return Stream.of(jo);
                    }
                })
                .filter( f -> ! environmentFilters.contains(f))
                .filter( f -> ! systemFieldFilters.contains(f.getAsJsonObject().get("field").getAsString()))
                .collect(JsonArray::new,
                        JsonArray::add,
                        JsonArray::addAll);
    }


}
