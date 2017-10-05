package com.rbmhtechnology.vind.solr.suggestion.result;

import com.rbmhtechnology.vind.solr.suggestion.SuggestionRequestHandler;
import com.rbmhtechnology.vind.solr.suggestion.params.SuggestionResultParams;
import org.apache.solr.common.util.NamedList;

import java.util.*;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SuggestionResultMulti implements SuggestionResult {

    private int count = 0;
    private int limit = Integer.MAX_VALUE;
    private SuggestionRequestHandler.LimitType limitType;
    private List<MultiFacet> suggestion_list = new ArrayList<MultiFacet>();

    public SuggestionResultMulti(int limit, SuggestionRequestHandler.LimitType limitType) {
        this.limitType = limitType;
        this.limit = limit;
    }

    public int getCount() {
        return suggestion_list.stream().mapToInt(suggestion -> suggestion.count).sum();
    }

    @Override
    public Object write() {
        Map<String,Object> suggestion_result = new HashMap<String, Object>();

        //sort results
        Collections.sort(suggestion_list,new Comparator<MultiFacet>() {
            @Override
            public int compare(MultiFacet multiFacet, MultiFacet multiFacet2) {
                return Integer.valueOf(multiFacet2.count).compareTo(multiFacet.count);
            }
        });

        //Crop results
        //TODO use limitType
        if(limit < Integer.MAX_VALUE && limit < suggestion_list.size()) {
            suggestion_list = suggestion_list.subList(0,limit);
        }

        suggestion_result.put(SuggestionResultParams.SUGGESTION_COUNT, suggestion_list.size());

        NamedList suggestions = new NamedList();

        for(MultiFacet mf : suggestion_list) {
            suggestions.add(mf.name.toLowerCase(), mf.write());
        }

        suggestion_result.put(SuggestionResultParams.SUGGESTION_FACETS,suggestions);
        return suggestion_result;
    }

    class MultiFacet {

        HashMap<String,HashMap<String,Integer>> facets = new HashMap<String,HashMap<String,Integer>>();
        String name;
        Integer count = Integer.MAX_VALUE;

        public void add(final String name, final String value,Integer count) {
            if(!facets.containsKey(name)) {
                facets.put(name,new HashMap<String,Integer>());
            }
            facets.get(name).put(value,count);
            this.name = this.name == null ? value : this.name + " " + value;
            this.count = Math.min(this.count,count);
        }

        public HashMap<String,Object> write() {

            HashMap<String,Object> out = new HashMap<String, Object>();

            out.put("count", count);
            out.put("facets",facets);

            return out;
        }

    }

    public MultiFacet createMultiFacet() {
        MultiFacet m = new MultiFacet();
        suggestion_list.add(m);
        return m;
    }
}
