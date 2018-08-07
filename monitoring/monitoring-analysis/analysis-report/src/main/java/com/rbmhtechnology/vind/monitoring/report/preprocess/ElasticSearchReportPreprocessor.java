/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.preprocess;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.configuration.ElasticSearchReportConfiguration;
import com.rbmhtechnology.vind.monitoring.report.util.SimilarityUtils;
import com.rbmhtechnology.vind.monitoring.utils.ElasticSearchClient;
import io.redlink.utils.ResourceLoaderUtils;
import io.searchbox.client.JestResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.monitoring.report.util.ReportLabels.*;

/**
 * Created on 11.06.18.
 */
public class ElasticSearchReportPreprocessor extends ReportPreprocessor {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchReportPreprocessor.class);
    private static final int MAX_LEVENSHTEIN_DISTANCE = 0;
    public static final int MAX_FILTER_DIFFERENCE = 1;

    private final Long from;
    private final Long to;

    private final  ElasticSearchClient elasticClient = new ElasticSearchClient();
    private final Long scrollSpan = 1000L;
    private DateTimeFormatter esDateFormater = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private List<JsonElement> environmentFilters = new ArrayList<>();
    private List<String> sessionIds = new ArrayList<>();

    private final ElasticSearchReportConfiguration configuration;

    public ElasticSearchReportPreprocessor(ElasticSearchReportConfiguration configuration, ZonedDateTime from, ZonedDateTime to) {
        this.from = from.toInstant().toEpochMilli();
        this.to = to.toInstant().toEpochMilli();

        this.configuration = configuration;
    }

    public ElasticSearchReportPreprocessor addSystemFilterField(String ... fields) {
        if (Objects.nonNull(fields)) {
            systemFieldFilters.addAll(Arrays.asList(fields));
        }
        return this;
    }

    @Override
    List<String> getSessionIds() {
        return this.sessionIds;
    }

    @Override
    void afterPreprocessing() {
        try {
            elasticClient.destroy();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.afterPreprocessing();
    }

    //Gets the list of sessions to preprocess and figures out the
    // general filters which apply to all queries.
    void beforePreprocessing() {

        elasticClient.init(
                configuration.getConnectionConfiguration().getEsHost(),
                configuration.getConnectionConfiguration().getEsPort(),
                configuration.getConnectionConfiguration().getEsIndex(),
                configuration.getEsEntryType(),
                ResourceLoaderUtils.getResourceAsPath("processMapping.json")
        );

        log.info("Getting sessions and common filter for the period [{} - {}]",from, to);

        //Filter to skip out previous processed entries.
        final String skipPreprocessed =
                ",\n\"must_not\":{\"exists\":{\"field\":\"%sprocess\"}}";

        final String query = elasticClient.loadQueryFromFile("prepare",
                this.scrollSpan,
                getEsFilters(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                configuration.isForcePreprocessing()? "" : String.format(skipPreprocessed, this.configuration.getMessageWrapper()),
                this.configuration.getMessageWrapper(),
                this.from,
                this.to);

        final Set<String> sessions =  new HashSet<>();

        String scrollId = null;
        Long start = 0L;
        Long totalResults = 1L;
        while (start < totalResults) {

            final JestResult scrollResult;
            if (Objects.nonNull(scrollId)) {
                scrollResult =  elasticClient.scrollResults(scrollId);
            } else {
                scrollResult = elasticClient.getScrollQuery(query);
                totalResults =scrollResult.getJsonObject().getAsJsonObject("hits").get("total").getAsLong();
            }

            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            final JsonArray scrollHits = scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");

            final List<JsonObject> vindEntries = Streams.stream(scrollHits)
                    //Get the elasticsearch source
                    .map(hit -> hit.getAsJsonObject()
                            .getAsJsonObject("_source"))
                    //Get the log out of the wrapper
                    .map( entry -> {
                        if (StringUtils.isNotBlank(this.configuration.getMessageWrapper())) {
                            return entry
                                    .getAsJsonObject(String.valueOf(this.configuration.getMessageWrapper().subSequence(0, (this.configuration.getMessageWrapper().length() - 1))));
                        } else return entry; })
                    .collect(Collectors.toList());

            //Get the session Ids for the first result scroll
            sessions.addAll(vindEntries.stream()
                    //Get the Session
                    .map( vindLog -> vindLog
                            .getAsJsonObject("session")
                            .get("sessionId").getAsString())
                    .collect(Collectors.toList()));



            start += scrollSpan;
        }

        if(Objects.nonNull(scrollId)) {
            elasticClient.closeScroll(scrollId);
        }

        sessionIds.addAll(sessions);
        log.info("{} different session IDs found on the period [{} - {}]", sessionIds.size(), from, to);

    }

    public Boolean preprocessSession(String sessionId) {

        //Filter to skip out previous processed entries.
        final String skipPreprocessed =
                ",\n\"must_not\":{\"exists\":{\"field\":\"%sprocess\"}}";

        log.debug("Starting pre-processing of vind monitoring entries for session [{}],",sessionId);
        //fetch all the entries for the session
        final String query = elasticClient.loadQueryFromFile("session",
                this.scrollSpan,
                getEsFilters(),
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                sessionId,
                configuration.isForcePreprocessing()? "" : String.format(skipPreprocessed, this.configuration.getMessageWrapper()));

        final ArrayList<JsonElement> commonFilters = new ArrayList<>();
        final Set<JsonObject> requests =  new HashSet<>();

        String scrollId = null;
        Long start = 0L;
        Long totalResults = 1L;
        while (start < totalResults) {
            final JestResult scrollResult;
            if (Objects.nonNull(scrollId)) {
                scrollResult =  elasticClient.scrollResults(scrollId);
            } else {
                scrollResult = elasticClient.getScrollQuery(query);
                totalResults =scrollResult.getJsonObject().getAsJsonObject("hits").get("total").getAsLong();
            }

            scrollId =
                    scrollResult.getJsonObject().getAsJsonPrimitive("_scroll_id").getAsString();

            final JsonArray scrollHits = scrollResult.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");

            final List<JsonObject> vindEntries = Streams
                    .stream(scrollHits.iterator())
                    //Get the elasticsearch vind monitoring object and add the es id
                    .map(hit -> {
                        final JsonObject entry ;
                        if (StringUtils.isNotBlank(this.configuration.getMessageWrapper())) {
                            entry = hit.getAsJsonObject()
                                    .getAsJsonObject("_source")
                                    .getAsJsonObject(String.valueOf(this.configuration.getMessageWrapper().subSequence(0, (this.configuration.getMessageWrapper().length() - 1))));
                        } else entry = hit.getAsJsonObject()
                                .getAsJsonObject("_source");

                        entry.addProperty("_id", hit.getAsJsonObject().get("_id").getAsString());
                        entry.addProperty("_index", hit.getAsJsonObject().get("_index").getAsString());
                        return entry;
                    })
                    .collect(Collectors.toList());

            //Find out filters common to all queries
            final List<JsonObject> findFulltextEntry = vindEntries.stream()
                    .filter(vindEntry ->vindEntry.get("type").getAsString().equals("fulltext"))
                    .collect(Collectors.toList());

            if(start ==0 && findFulltextEntry.size() > 0){
                //Find out filters common to all queries
                final JsonElement initialFilters = findFulltextEntry.get(0)
                        .getAsJsonObject("request")
                        .get("filter");

                commonFilters.addAll(Lists.newArrayList(extractFilterFields(initialFilters).iterator()));
            }

            findFulltextEntry.stream()
                    .map(fulltextVindEntry ->
                            extractFilterFields(fulltextVindEntry.getAsJsonObject("request").get("filter")))
                    .forEach( hfs ->
                            commonFilters.retainAll(Lists.newArrayList(hfs.iterator()))
                    );

            requests.addAll( vindEntries.stream()
                    .map( e ->{
                        e.remove("application");
                        e.remove("session");
                        e.remove("response");
                        e.remove("metadata");
                        return e;
                    })
                    .collect(Collectors.toList()));

            start += scrollSpan;

        }

        //close scroll
        if(Objects.nonNull(scrollId)) {
            elasticClient.closeScroll(scrollId);
        }

        environmentFilters.addAll(commonFilters);
        log.debug("{} different environment filter found for the session [{}]", environmentFilters.size(), sessionId);

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

        log.debug("A total of {} vind monitoring entries for session [{}],",sortedRequest.size() ,sessionId);
        processSession(sortedRequest);

        environmentFilters.removeAll(commonFilters);

        return bulkUpdate(requests);
    }

    private void processSession(List<JsonObject> entries) {

        //Exclude suggestion entries
        final List<JsonObject> cleanList = entries.stream()
                .filter( e -> !e.get("type").getAsString().equals("suggestion"))
                .collect(Collectors.toList());

        JsonObject lastQuery = null;
        final List<JsonObject> lastAccesses = new ArrayList<>();
        int searchStep = 1;

        for (final JsonObject actual : cleanList) {

            //Add pre-processing info
            final JsonObject process = new JsonObject();
            actual.add(SEARCH_PRE_PROCESS_RESULT, process);

            //INTERACTIONS
            if (actual.get("type").getAsString().equals("interaction")) {
                final JsonObject access = actual.getAsJsonObject("request");
                if (access.get("action").getAsString().equals("select")) {

                    //Check duplicated accesses
                    if (CollectionUtils.isNotEmpty(lastAccesses)
                            && lastAccesses.contains(access)) {
                        process.addProperty(SEARCH_DUPLICATE, true);
                        process.addProperty(SEARCH_SKIP, true);

                    } else {
                        lastAccesses.add(access);
                        if (Objects.nonNull(lastQuery)) {
                            final JsonObject processInfo = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT);
                            processInfo.addProperty(SEARCH_INTERACTION_SELECT, processInfo.get(SEARCH_INTERACTION_SELECT).getAsLong() + 1);
                        } else {
                            process.addProperty(SEARCH_SKIP, true);
                        }
                    }
                }
            }

            //FULLTEXT QUERIES
            if (actual.get("type").getAsString().equals("fulltext")) {
                //When the last query had interaction in between we start from scratch
                if (Objects.nonNull(lastQuery)
                        && lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).get(SEARCH_INTERACTION_SELECT).getAsLong() > 0) {
                    //TODO:Is the select interaction the end of a user query flow?
                    searchStep = 1;
                    lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).addProperty(SEARCH_FINAL_QUERY, true);
                    lastQuery = null;
                }
                //empty check
                if (isEmptyQuery(actual)) {
                    log.debug("Empty entry: Resetting step count to 1 and lastQuery to null.");
                    process.addProperty(SEARCH_SKIP, true);
                    process.addProperty(SEARCH_EMPTY, true);
                    //empty query is the end of an user iteration
                    searchStep = 1;
                    if (Objects.nonNull(lastQuery)) {
                        lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).addProperty(SEARCH_FINAL_QUERY, true);
                        lastQuery = null;
                    }
                } else {
                    //check if it is a duplicated query
                    if (Objects.nonNull(lastQuery) && isDuplicatedQuery(actual, lastQuery)) {
                        log.debug("Duplicated entry: checking if is a paging, sorting or duplicated.");
                        if (isPaging(actual, lastQuery)) {
                            process.addProperty(SEARCH_PAGING, true);
                        }
                        if (isSorting(actual, lastQuery)) {
                            process.addProperty(SEARCH_SORTING, true);
                        }
                        if (!(isSorting(actual, lastQuery) || isPaging(actual, lastQuery))) {
                            process.addProperty(SEARCH_DUPLICATE, true);
                        }

                        process.addProperty(SEARCH_SKIP, true);

                    } else {
                        //Clear list of accesses
                        lastAccesses.clear();

                        //Initialize process result json object
                        process.addProperty(SEARCH_INTERACTION_SELECT, 0);
                        process.add(SEARCH_STEPS, new JsonObject());
                        process.addProperty(SEARCH_FINAL_QUERY, true);


                        //Calculate flattened list of filters
                        //TODO: do not ignore conditional filters
                        final JsonElement filters = actual.getAsJsonObject("request").get("filter");
                        final JsonArray flattenedFilters = new JsonArray();
                        flattenedFilters.addAll(extractFilterFields(filters));
                        process.add(SEARCH_FILTERS, flattenedFilters);

                        final JsonArray stepFilters = new JsonArray();
                        stepFilters.addAll(flattenedFilters);
                        if (Objects.nonNull(lastQuery)) {
                            if (isRefinedQuery(actual, lastQuery)) {
                                //set previous as not final
                                lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).addProperty(SEARCH_FINAL_QUERY, false);
                                //Copy previous steps info into this query
                                final JsonObject previousSteps = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).get(SEARCH_STEPS).getAsJsonObject();
                                final JsonObject copy = deepCopy(previousSteps);
                                process.add(SEARCH_STEPS, copy);

                                //Select new filters for this step
                                final JsonArray lastFilters = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).getAsJsonArray(SEARCH_FILTERS);
                                Streams.stream(flattenedFilters.iterator())
                                        .filter(lastFilters::contains)
                                        .forEach(stepFilters::remove);
                                searchStep++;
                                lastQuery = actual;
                            } else {
                                searchStep = 1;
                                lastQuery = actual;
                            }
                        } else {
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

    private Boolean isDuplicatedQuery (JsonObject query, JsonObject lastQuery) {

        final String oldQueryText = lastQuery.getAsJsonObject("request").get("query").getAsString();
        final String actualQueryText = query.getAsJsonObject("request").get("query").getAsString();

        if(actualQueryText.equals(oldQueryText)) {
            final JsonElement previousFilters = lastQuery.getAsJsonObject("request").get("filter");
            final JsonElement actualFilters = query.getAsJsonObject("request").get("filter");
            return equalFilters(actualFilters, previousFilters);
        }
        return false;
    }

    private Boolean isPaging (JsonObject query, JsonObject lastQuery) {

        if(isDuplicatedQuery(query,lastQuery)){
            final JsonObject oldPaging = lastQuery.getAsJsonObject("paging");
            final JsonObject actualPaging = query.getAsJsonObject("paging");
            return !actualPaging.equals(oldPaging);
        }
        return false;
    }

    private Boolean isSorting (JsonObject query, JsonObject lastQuery) {

        if(isDuplicatedQuery(query,lastQuery)){
            final JsonArray oldSorting = lastQuery.getAsJsonArray("sorting");
            final JsonArray actualSorting = query.getAsJsonArray("sorting");
            return !actualSorting.equals(oldSorting);
        }
        return false;
    }

    private Boolean isEmptyQuery (JsonObject query) {

        final String queryText = query.getAsJsonObject("request").get("query").getAsString();
        if (queryText.equals("*")) {
            final JsonArray queryFilters = extractFilterFields(query.getAsJsonObject("request").get("filter"));
            return queryFilters.size() == 0;
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
            return (previousFilters.size() - intersectionCount) <= MAX_FILTER_DIFFERENCE;
        }
        return false;
    }

    private Boolean isSimilarTextQuery (String text, String lastText) {
        if(text.equals(lastText)) {
            return true;
        } else  if (text.contains(lastText)) {
            return true;
        } else return SimilarityUtils.levenshteinDistance(text, lastText) <= MAX_LEVENSHTEIN_DISTANCE;
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

    private Boolean equalFilters(JsonElement fs1, JsonElement fs2) {
        //Prepare element 1
        final JsonArray filters1 = new JsonArray();
        if (fs1.isJsonObject()) {
            filters1.add(fs1.getAsJsonObject());
        } else {
            filters1.addAll(fs1.getAsJsonArray());
        }

        //Prepare element 2
        final JsonArray filters2 = new JsonArray();
        if (fs2.isJsonObject()) {
            filters2.add(fs2.getAsJsonObject());
        } else {
            filters2.addAll(fs2.getAsJsonArray());
        }

        return equalFilters(filters1,filters2);
    }

    private Boolean equalFilters(JsonArray fs1, JsonArray fs2) {
        //compare element size
        if(fs1.size() == fs2.size()){
            //Check if every filter is in the second list of filters
            return Streams.stream(fs1.iterator()).allMatch( f1 ->
                    Streams.stream(fs2.iterator())
                            .anyMatch( f -> equalFilters(f.getAsJsonObject(),f1.getAsJsonObject()))
            );
        }

        return false;
    }

    private Boolean equalFilters(JsonObject f1, JsonObject f2) {
        if (f1.get("type").getAsString().equals(f2.get("type").getAsString())
                && f1.get("scope").getAsString().equals(f2.get("scope").getAsString())) {
            if(f1.has("delegates") && f2.has("delegates")) {

                return equalFilters(f1.get("delegates"),f2.get("delegates"));
            } else {
                return f1.equals(f2);
            }
        }
        return false;
    }

    private JsonObject deepCopy(JsonObject object) {
        final JsonObject copy = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject().entrySet()) {
            copy.add(entry.getKey(), deepCopy(entry.getValue()));
        }

        return copy;
    }

    private JsonElement deepCopy(JsonElement object) {

        if (object.isJsonObject()) {
            return object;
        }
        else {
            final JsonArray copy = new JsonArray();
            for (JsonElement o : object.getAsJsonArray()) {
                copy.add(deepCopy(o));
            }
            return copy;
        }
    }

    private Boolean bulkUpdate(Set<JsonObject> results) {
        final List<JsonObject> processResults = results.stream()
                .filter( je -> {
                    if (StringUtils.isNotBlank(this.configuration.getMessageWrapper())) {
                       return je.has("process");
                    } else {
                        return je.getAsJsonObject(this.configuration.getMessageWrapper()).has("process");
                    }
                })
                .map( e -> {
                    final JsonObject update = new JsonObject();
                    if (StringUtils.isNotBlank(this.configuration.getMessageWrapper())) {
                        final JsonObject result =  new JsonObject();
                        result.add(SEARCH_PRE_PROCESS_RESULT, e.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT));
                        update.add(this.configuration.getMessageWrapper().substring(0,this.configuration.getMessageWrapper().length()-1),result);
                    } else {
                        update.add(SEARCH_PRE_PROCESS_RESULT, e.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT));
                    }
                    update.addProperty("_id", e.get("_id").getAsString());
                    update.addProperty("_index", e.get("_index").getAsString());
                    return update;
                })
                .collect(Collectors.toList());

        log.debug("Writing {} processing results to elasticsearch",processResults.size());
        elasticClient.bulkUpdate(processResults, this.configuration.getEsEntryType());
        return true;
    }

    private String getEsFilters(){
        if(configuration.getEsFilters().size() > 0 ) {
            final String jsonMatchFilterPattern = "{\"match\":{\"%s\":\"%s\"}}";
            final String filters = configuration.getEsFilters().entrySet().stream()
                    .map(e -> String.format(jsonMatchFilterPattern, e.getKey(), e.getValue()))
                    .collect(Collectors.joining(",\n"));

            return filters + ",\n";
        } else return "";
    }
}
