package com.rbmhtechnology.vind.solr.suggestion;

import com.rbmhtechnology.vind.solr.suggestion.params.SuggestionRequestParams;
import com.rbmhtechnology.vind.solr.suggestion.service.SuggestionService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://svn.apache.org/repos/asf/lucene/solr/branches/branch-1.4/src/java/org/apache/solr/handler/
 *
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SuggestionRequestHandler extends SearchHandler implements SolrCoreAware {

    private static final String DEFAULT_END_VALUE = "NOW/DAY";
    private static final double DEFAULT_SCORE_VALUE = 1D;

    public enum Type {
        single,
        multi,
        mixed;

        public static Type parse(String s, Type def) {
            try {
                return valueOf(s);
            } catch (NullPointerException | IllegalArgumentException e) {
                return def;
            }
        }
    }

    public enum Strategy {
        exact,
        permutate;

        public static Strategy parse(String s, Strategy def) {
            try {
                return valueOf(s);
            } catch (NullPointerException | IllegalArgumentException e) {
                return def;
            }
        }
    }

    public enum LimitType {
        all,
        each;

        public static LimitType parse(String s, LimitType def) {
            try {
                return valueOf(s);
            } catch (NullPointerException | IllegalArgumentException e) {
                return def;
            }
        }
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SuggestionService suggestionService;

    private static Strategy STRATEGY = Strategy.permutate;
    private static boolean SUGGESTION = true;
    private static String DF = null;
    private static String[] FIELDS = null;
    private static String[] MULTIVALUE_FIELDS = null;
    private static String[] FQS = null;
    private static int TERM_LIMIT = 10;
    private static int LIMIT = Integer.MAX_VALUE;
    private static LimitType LIMIT_TYPE = LimitType.all;

    private static boolean SUGGESTION_INTERVAL = false;
    private static boolean SUGGESTION_INTERVAL_OTHER = false;

    public void inform(SolrCore core) {
        super.inform(core);
        suggestionService = new SuggestionService(core,this.getInitArgs());

        //set default args
        NamedList args = (NamedList)this.getInitArgs().get("defaults");

        SUGGESTION = args.get(SuggestionRequestParams.SUGGESTION) != null ?
                Boolean.parseBoolean((String)args.get(SuggestionRequestParams.SUGGESTION)) : SUGGESTION;
        TERM_LIMIT = args.get(SuggestionRequestParams.SUGGESTION_TERM_LIMIT) != null ?
                Integer.parseInt((String)args.get(SuggestionRequestParams.SUGGESTION_TERM_LIMIT)) : TERM_LIMIT;

        LIMIT = args.get(SuggestionRequestParams.SUGGESTION_LIMIT) != null ?
                Integer.parseInt((String)args.get(SuggestionRequestParams.SUGGESTION_LIMIT)) : LIMIT;

        LIMIT_TYPE = args.get(SuggestionRequestParams.SUGGESTION_LIMIT_TYPE) != null ?
                LimitType.parse((String) args.get(SuggestionRequestParams.SUGGESTION_LIMIT_TYPE), LIMIT_TYPE) : LIMIT_TYPE;

        DF = args.get(SuggestionRequestParams.SUGGESTION_DF) != null ?
                (String) args.get(SuggestionRequestParams.SUGGESTION_DF) : DF;

        STRATEGY = args.get(SuggestionRequestParams.SUGGESTION_STRATEGY) != null ?
                Strategy.parse((String)args.get(SuggestionRequestParams.SUGGESTION_STRATEGY), STRATEGY) :STRATEGY;

        List<String> fields = args.getAll(SuggestionRequestParams.SUGGESTION_FIELD) != null ?
                args.getAll(SuggestionRequestParams.SUGGESTION_FIELD) : Collections.emptyList();
        if(!fields.isEmpty()) {
            FIELDS = fields.toArray(new String[fields.size()]);
        }

        List<String> multivalue_fields = args.getAll(SuggestionRequestParams.SUGGESTION_MULTIVALUE_FIELD) != null ?
                args.getAll(SuggestionRequestParams.SUGGESTION_MULTIVALUE_FIELD) : Collections.emptyList();
        if(!multivalue_fields.isEmpty()) {
            MULTIVALUE_FIELDS = fields.toArray(new String[multivalue_fields.size()]);
        }

        List<String> fqs = args.getAll(CommonParams.FQ) != null ?
                args.getAll(CommonParams.FQ) : Collections.emptyList();
        if(!fqs.isEmpty()) {
            FQS = fqs.toArray(new String[fields.size()]);
        }

    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {

        final SolrParams params = req.getParams();

        if(params.getBool(SuggestionRequestParams.SUGGESTION,SUGGESTION)) {

            String q = params.get(CommonParams.Q);
            if(q == null) {
                rsp.add("error",error(400, "SuggestionRequest needs to have a 'q' parameter"));
                return;
            }

            String[] single_fields = params.getParams(SuggestionRequestParams.SUGGESTION_FIELD) != null ? params.getParams(SuggestionRequestParams.SUGGESTION_FIELD) : FIELDS;

            String[] multivalue_fields = params.getParams(SuggestionRequestParams.SUGGESTION_MULTIVALUE_FIELD) != null ? params.getParams(SuggestionRequestParams.SUGGESTION_MULTIVALUE_FIELD) : MULTIVALUE_FIELDS;

            if(single_fields == null && multivalue_fields == null) {
                rsp.add("error",error(400,"SuggestionRequest needs to have at least one 'suggestion.field' parameter or one 'suggestion.multivalue.field' parameter defined."));
                return;
            }

            int termLimit = params.getInt(SuggestionRequestParams.SUGGESTION_TERM_LIMIT,TERM_LIMIT);
            if(termLimit < 1) {
                rsp.add("error",error(400,"SuggestionRequest needs to have a 'suggestion.term.limit' greater than 0"));
                return;
            }

            int limit = params.getInt(SuggestionRequestParams.SUGGESTION_LIMIT,LIMIT);
            if(limit < 1) {
                rsp.add("error",error(400,"SuggestionRequest needs to have a 'suggestion.limit' greater than 0"));
                return;
            }

            String df = params.get(SuggestionRequestParams.SUGGESTION_DF,DF);
            if(df == null) {
                rsp.add("error",error(400,"SuggestionRequest needs to have a 'df' parameter"));
                return;
            }

            final Strategy strategy = Strategy.parse(params.get(SuggestionRequestParams.SUGGESTION_STRATEGY, null), STRATEGY);

            final LimitType limitType = LimitType.parse(params.get(SuggestionRequestParams.SUGGESTION_LIMIT_TYPE, null), LIMIT_TYPE);

            final String[] fqs = params.getParams(CommonParams.FQ) != null ? params.getParams(CommonParams.FQ) : FQS;

            Type type;

            if(single_fields != null && multivalue_fields == null) {
                type = Type.single;
            } else if(single_fields == null) {
                type = Type.multi;
                rsp.add("warning",error(410,"Multivalue suggestions are deprecated and will not be supported in further versions"));
                //return;
            } else {
                type = Type.mixed;
                rsp.add("warning",error(410,"Multivalue suggestions are deprecated and will not be supported in further versions"));
            }

            final String[] fields = (String[]) ArrayUtils.addAll(single_fields,multivalue_fields);

            ///////////////////////
            //Suggestion Intervals
            ///////////////////////
            final Map<String,Map<String,Object>> rangesMap = new HashMap<>();
            final String intervalField = params.get(SuggestionRequestParams.SUGGESTION_INTERVAL_FIELD);

            if(params.getBool(SuggestionRequestParams.SUGGESTION_INTERVAL,SUGGESTION_INTERVAL)) {
                final String[] ranges = params.getParams(SuggestionRequestParams.SUGGESTION_INTERVAL_LABEL);
                if(ranges == null || ranges.length <= 0) {
                    rsp.add("error",error(400,
                            "SuggestionRequest needs to have at least one '"+SuggestionRequestParams.SUGGESTION_INTERVAL_LABEL+"' parameter to create intervals"));
                    return;
                }

                if(StringUtils.isEmpty(intervalField)){
                    rsp.add("error",error(400,
                            "SuggestionRequest needs to have a '"+ SuggestionRequestParams.SUGGESTION_INTERVAL_FIELD +"' parameter to create intervals"));
                    return;
                }

                final Boolean other = params.getBool(SuggestionRequestParams.SUGGESTION_INTERVAL_OTHER,SUGGESTION_INTERVAL_OTHER);

                for (int i=0 ; i < ranges.length; i++){
                    final String label = ranges[i];
                    final Map<String,Object> rangeConfigurations = new HashMap<>();

                    final String rangeStartParam = String.format(SuggestionRequestParams.SUGGESTION_INTERVAL_RANGE_START, label);
                    final Object startValue = params.get(rangeStartParam);
                    if(startValue == null){
                        rsp.add("error",error(400,
                                "SuggestionRequest needs to have a '"+String.format(SuggestionRequestParams.SUGGESTION_INTERVAL_RANGE_START, label)+"' parameter to create an interval"));
                        return;
                    }
                    rangeConfigurations.put("start",startValue);

                    final String rangeEndParam = String.format(SuggestionRequestParams.SUGGESTION_INTERVAL_RANGE_END, label);
                    final Object endValue = params.get(rangeEndParam, DEFAULT_END_VALUE);
                    rangeConfigurations.put("end",endValue);

                    final String rangeScoreParam = String.format(SuggestionRequestParams.SUGGESTION_INTERVAL_RANGE_SCORE, label);
                    double scoreValue = params.getDouble(rangeScoreParam, DEFAULT_SCORE_VALUE);
                    rangeConfigurations.put("score",scoreValue);

                    rangesMap.put(label,rangeConfigurations);
                }

            }

            logger.debug("Get suggestions for query '{}', type: {}, fqs: {}", q, type, fqs != null ? StringUtils.join(fqs, ",") : "none");

            suggestionService.run(rsp, params, q, df, fields, single_fields, multivalue_fields, fqs,termLimit, limit, limitType, type, strategy,intervalField,rangesMap);

        } else {
            super.handleRequestBody(req,rsp);
        }
    }

    private HashMap<String,Object> error(int code,String msg) {
        final HashMap<String,Object> error = new HashMap<String,Object>();
        error.put("msg",msg);
        error.put("code",code);
        return error;
    }


    @Override
    public String getDescription() {
        return "This handler creates suggestions for a faceted search";
    }

    @Override
    public String getSource() {
        return "no source";
    }

    @Override
    public String getVersion() {
        return "3.0-SNAPSHOT";
    }
}
