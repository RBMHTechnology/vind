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
import io.searchbox.client.JestResult;
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
public class ElasticSearchReportPreprocessor extends ReportPreprocessor {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchReportPreprocessor.class);

    private final Long from;
    private final Long to;

    private final  ElasticSearchClient elasticClient = new ElasticSearchClient();
    private Long scrollSpan = 1000l;
    private DateTimeFormatter esDateFormater = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private List<JsonElement> environmentFilters = new ArrayList<>();
    private List<String> sessionIds = new ArrayList<>();

    private final ElasticSearchReportConfiguration configuration;

    public ElasticSearchReportPreprocessor(ElasticSearchReportConfiguration configuration, ZonedDateTime from, ZonedDateTime to) {
        this.from = from.toInstant().toEpochMilli();
        this.to = to.toInstant().toEpochMilli();

        elasticClient.init(
                configuration.getConnectionConfiguration().getEsHost(),
                configuration.getConnectionConfiguration().getEsPort(),
                configuration.getConnectionConfiguration().getEsIndex(),
                configuration.getEsEntryType(),
                ElasticSearchReportPreprocessor.class.getClassLoader().getResource("processMapping.json").getPath()
        );

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

    //Gets the list of sessions to preprocess and figures out the
    // general filters which apply to all queries.
    void beforePreprocessing() {
        log.info("Getting sessions and common filter for the period [{} - {}]",from, to);

        final String query = elasticClient.loadQueryFromFile("prepare",
                this.scrollSpan,
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.configuration.getMessageWrapper(),
                this.from,
                this.to);

        final Set<String> sessions =  new HashSet<>();
        final ArrayList<JsonElement> commonFilters = new ArrayList<>();

        String scrollId = null;
        Long start = 0l;
        Long totalResults = 1l;
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

            //Find out filters common to all queries
            if(start ==0 && vindEntries.size() > 0){
                //Find out filters common to all queries
                final JsonElement initialFilters = vindEntries.get(0)
                        .getAsJsonObject("request")
                        .get("filter");

                commonFilters.addAll(Lists.newArrayList(extractFilterFields(initialFilters).iterator()));
            }

            vindEntries.stream()
                    .map(vindEntry ->
                            extractFilterFields(vindEntry.getAsJsonObject("request").get("filter")))
                    .forEach( hfs ->
                            commonFilters.retainAll(Lists.newArrayList(hfs.iterator()))
                    );

            start += scrollSpan;
        }

        sessionIds.addAll(sessions);
        log.info("{} different session IDs found on the period [{} - {}]", sessionIds.size(), from, to);
        environmentFilters.addAll(commonFilters);
        log.debug("{} different environment filter found for the period [{} - {}]", environmentFilters.size(), from, to);
    }

    public Boolean preprocessSession(String sessionId) {

        log.info("Starting pre-processing of vind monitoring entries for session [{}],",sessionId);
        //fetch all the entries for the session
        final String query = elasticClient.loadQueryFromFile("session",
                this.scrollSpan,
                this.configuration.getMessageWrapper(),
                this.configuration.getApplicationId(),
                this.configuration.getMessageWrapper(),
                sessionId,
                this.configuration.getMessageWrapper());

        final Set<JsonObject> requests =  new HashSet<>();

        String scrollId = null;
        Long start = 0l;
        Long totalResults = 1l;
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

        log.info("A total of {} vind monitoring entries for session [{}],",sortedRequest.size() ,sessionId);
        processSession(sortedRequest);

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
                    log.debug("Empty entry: Resetting step count to 1 and lastQuery to null.");
                    process.addProperty(SEARCH_SKIP,true);
                    process.addProperty(SEARCH_EMPTY,true);
                    //empty query is the end of an user iteration
                    searchStep = 1;
                    lastQuery = null;
                } else {
                    //check if it is a duplicated query
                    if(Objects.nonNull(lastQuery) && isDuplicatedQuery(actual, lastQuery)) {
                        log.debug("Duplicated entry: checking if is a paging, sorting or duplicated.");
                        if(isPaging(actual,lastQuery)) {
                            process.addProperty(SEARCH_PAGING, true);
                        }
                        if(isSorting(actual,lastQuery)) {
                            process.addProperty(SEARCH_SORTING, true);
                        }
                        if (!(isSorting(actual,lastQuery) || isPaging(actual,lastQuery))) {
                            process.addProperty(SEARCH_DUPLICATE, true);
                        }

                        process.addProperty(SEARCH_SKIP,true);

                    } else {
                        //Clear list of accesses
                        lastAccesses.clear();

                        //Initialize process result json object
                        process.addProperty(SEARCH_INTERACTION_SELECT, 0);
                        process.add(SEARCH_STEPS, new JsonObject());

                        //Calculate flattened list of filters
                        //TODO: do not ignore conditional filters
                        final JsonElement filters = actual.getAsJsonObject("request").get("filter");
                        final JsonArray flattenedFilters = new JsonArray();
                        flattenedFilters.addAll(extractFilterFields(filters));
                        process.add(SEARCH_FILTERS, flattenedFilters);

                        final JsonArray stepFilters = new JsonArray();
                        stepFilters.addAll(flattenedFilters);
                        if(Objects.nonNull(lastQuery)) {
                            if(isRefinedQuery(actual,lastQuery)) {
                                //Copy previous steps info into this query
                                final JsonObject previousSteps = lastQuery.getAsJsonObject(SEARCH_PRE_PROCESS_RESULT).get(SEARCH_STEPS).getAsJsonObject();
                                final JsonObject copy = deepCopy(previousSteps);


                                process.add(SEARCH_STEPS, copy);

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

    private Boolean isDuplicatedQuery (JsonObject query, JsonObject lastQuery) {

        final String oldQueryText = lastQuery.getAsJsonObject("request").get("query").getAsString();
        final String actualQueryText = query.getAsJsonObject("request").get("query").getAsString();

        if(actualQueryText.equals(oldQueryText)) {
            final JsonElement previousFilters = lastQuery.getAsJsonObject("request").get("filter");
            final JsonElement actualFilters = query.getAsJsonObject("request").get("filter");
            if(equalFilters(actualFilters, previousFilters)){
                return true;
            }
        }
        return false;
    }

    private Boolean isPaging (JsonObject query, JsonObject lastQuery) {

        if(isDuplicatedQuery(query,lastQuery)){
            final JsonObject oldPaging = lastQuery.getAsJsonObject("paging");
            final JsonObject actualPaging = query.getAsJsonObject("paging");
            if(!actualPaging.equals(oldPaging)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isSorting (JsonObject query, JsonObject lastQuery) {

        if(isDuplicatedQuery(query,lastQuery)){
            final JsonArray oldSorting = lastQuery.getAsJsonArray("sorting");
            final JsonArray actualSorting = query.getAsJsonArray("sorting");
            if(!actualSorting.equals(oldSorting)) {
                return true;
            }
        }
        return false;
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

        final Iterator members = object.getAsJsonObject().entrySet().iterator();
        while(members.hasNext()) {
            Map.Entry<String, JsonElement> entry = (Map.Entry)members.next();
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
            final Iterator jsonElements = object.getAsJsonArray().iterator();
            while(jsonElements.hasNext()) {
                copy.add(deepCopy((JsonElement) jsonElements.next()));
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

        log.info("Writing {} processing results to elasticsearch",processResults.size());
        elasticClient.bulkUpdate(processResults, this.configuration.getEsEntryType());
        return true;
    }

}
