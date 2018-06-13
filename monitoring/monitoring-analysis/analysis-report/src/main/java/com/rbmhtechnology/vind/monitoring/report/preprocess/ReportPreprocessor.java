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

    private List<JsonObject> environmentFilters;

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


    public List<String> getSessionsIncluded() {

        final String query = elasticClient.loadQueryFromFile("sessionIds",
                this.scrollSpan,
                this.messageWrapper,
                this.messageWrapper,
                this.appId,
                this.messageWrapper,
                this.messageWrapper,
                this.from,
                this.to);

        final JestResult searchResult = elasticClient.getScrollQuery(query);

        String scrollId =
                searchResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();
        final Long totalResults = ((SearchResult) searchResult).getTotal();

        final JsonArray hits = searchResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
        final Set<String> sessions =  new HashSet<>();

        sessions.addAll(Streams.stream(hits)
                .map(hit -> hit.getAsJsonObject().getAsJsonObject("fields").getAsJsonArray(this.messageWrapper + "session.sessionId").get(0).getAsString())
                .collect(Collectors.toList()));

        Long start = scrollSpan;
        while (start < totalResults) {

            final JestResult scrollResult =  elasticClient.scrollResults(scrollId);
            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            final JsonArray scrollHits = scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            sessions.addAll(Streams.stream(scrollHits)
                    .map(hit -> hit.getAsJsonObject().getAsJsonObject("fields").getAsJsonArray(this.messageWrapper + "session.sessionId").get(0).getAsString())
                    .collect(Collectors.toList()));
            start += scrollSpan;

        }

        return Lists.newArrayList(sessions);
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
        final Long totalResults = ((SearchResult) searchResult).getTotal();

        final JsonArray hits = searchResult.getJsonObject().getAsJsonObject("hits")
                .getAsJsonArray("hits");

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
                        process.addProperty("duplicate",true);

                    } else {
                        lastAccesses.add(access);
                        if (Objects.nonNull(lastQuery)) {
                            final JsonObject processInfo = lastQuery.getAsJsonObject("process");
                            processInfo.addProperty("access", processInfo.get("access").getAsLong() + 1);
                        } else {
                            process.addProperty("skip",true);
                        }
                    }
                    searchStep = 1; //TODO: move to incremental check
                    lastQuery = null;
                }
            }

            //FULLTEXT QUERIES
            if (actual.get("type").getAsString().equals("fulltext")) {
                //Initialize interactions to 0
                process.addProperty(SEARCH_INTERACTION_SELECT, 0);
                process.add("steps", new JsonObject());

                //check if it is a duplicated query
                if(Objects.nonNull(lastQuery) && lastQuery.getAsJsonObject("request").equals(actual.getAsJsonObject("request"))) {
                    process.addProperty(SEARCH_DUPLICATE, true);
                    //TODO: we do not care whether it is a paging action or a sorting action

                } else {
                    //Clear list of accesses
                    lastAccesses.clear();

                    //Calculate flattened list of filters
                    JsonArray flattenedFilters = new JsonArray();
                    final JsonElement filters = actual.getAsJsonObject("request").get("filter");
                    if (filters.isJsonArray()) {
                        flattenedFilters.addAll(extractFilterFields(filters.getAsJsonArray()));
                        process.add("filters", flattenedFilters);
                    } else {
                        final JsonArray fs = new JsonArray();
                        fs.add(filters);
                        flattenedFilters.addAll(extractFilterFields(fs));
                        process.add("filters", flattenedFilters);
                    }

                    final JsonArray stepFilters = new JsonArray();
                    stepFilters.addAll(flattenedFilters);
                    if(Objects.nonNull(lastQuery)) {
                        if(isRefinedQuery(actual,lastQuery)) {
                            //Copy previous steps info into this query
                            process.add("steps", lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).get("steps"));

                            final JsonArray lastFilters = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray("filters");
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

                    process.getAsJsonObject("steps").add(String.valueOf(searchStep), stepFilters);
                    process.addProperty(SEARCH_STEP, searchStep);
                    process.addProperty(SEARCH_STRING, actual.getAsJsonObject("request").get("query").getAsString());
                }
            }
        }

    }

    private Boolean isRefinedQuery (JsonObject query, JsonObject lastQuery) {


        final String oldQueryText = lastQuery.getAsJsonObject("request").get("query").getAsString();
        final String actualQueryText = query.getAsJsonObject("request").get("query").getAsString();

        if(isSimilarTextQuery(actualQueryText, oldQueryText)) {
            final JsonArray previousFilters = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray("filters");
            final JsonArray actualFilters = query.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray("filters");
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

    private JsonArray extractFilterFields(JsonArray filters) {
        return Streams.stream(filters.iterator())
                .map(JsonElement::getAsJsonObject)
                .flatMap( jo -> {
                    if (jo.has("delegates")){
                        final JsonElement delegate = jo.get("delegates");
                        if (delegate.isJsonArray()) {
                            return Streams.stream(extractFilterFields(delegate.getAsJsonArray()).iterator());
                        } else {
                            return Stream.of(delegate.getAsJsonObject());
                        }
                    } else {
                        return Stream.of(jo);
                    }
                })
                .collect(JsonArray::new,
                        JsonArray::add,
                        JsonArray::addAll);
    }



    private Boolean equalFilters(JsonObject f1, JsonObject f2) {
        if (f1.get("type").getAsString().equals(f2.get("type").getAsString())
                && f1.get("scope").getAsString().equals(f2.get("scope").getAsString())) {
            if(f1.has("delegates") && f2.has("delegates")) {
                if(f1.get("delegates").isJsonArray() && f2.get("delegates").isJsonArray()) {
                    return
                        equalFilters(f1.getAsJsonArray("delegates"), f2.getAsJsonArray("delegates"));
                } else
                    return equalFilters(f1.getAsJsonObject("delegates"), f2.getAsJsonObject("delegates"));
            } else {
                return f1.equals(f2);
            }
        }
        return false;
    }
    private Boolean equalFilters(JsonArray fs1, JsonArray fs2) {
        //compare element size
        if(fs1.size() == fs2.size()){
            final Iterator<JsonElement> delegates = fs1.iterator();
            //Check if every filter is in the second list of filters

            return Streams.stream(delegates).allMatch( f1 ->
                    Streams.stream(fs2.iterator())
                            .anyMatch( f -> equalFilters(f.getAsJsonObject(),f1.getAsJsonObject()))
            );
        }

        return false;
    }

}
