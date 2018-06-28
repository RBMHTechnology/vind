package com.rbmhtechnology.vind.solr.suggestion.result;

import java.util.*;

import com.rbmhtechnology.vind.solr.suggestion.params.SuggestionResultParams;
import com.rbmhtechnology.vind.solr.suggestion.SuggestionRequestHandler;
import org.apache.solr.common.util.NamedList;


/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SuggesionResultSingle implements SuggestionResult {

    private int count = 0;
    private int limit = Integer.MAX_VALUE;
    private SuggestionRequestHandler.LimitType limitType;
    private HashMap<String,List<Facet>> fields = new HashMap<>();

    private static final Comparator<Facet> COUNT_SORTER = Comparator.naturalOrder();

    public SuggesionResultSingle(int limit, SuggestionRequestHandler.LimitType limitType) {
        this.limit = limit;
        this.limitType = limitType;
    }

    public Object write() {
        NamedList<Object> suggestions = new NamedList<>();

        NamedList<Object> suggestion_facets = new NamedList<>();

        //sort results
        for(String field : fields.keySet()) {
            Collections.sort(fields.get(field), COUNT_SORTER);
        }

        //crop results
        if(limit < Integer.MAX_VALUE) {
            cropResult();
        }

        //put results in result structure
        for(String field : fields.keySet()) {
            NamedList facets = new NamedList();

            for(Facet facet : fields.get(field)) {
                facets.add(facet.value,facet.count);
                count++;
            }

            suggestion_facets.add(field,facets);
        }

        suggestions.add(SuggestionResultParams.SUGGESTION_COUNT, count);

        if(count>0) {
            suggestions.add(SuggestionResultParams.SUGGESTION_FACETS, suggestion_facets);
        }

        return suggestions;
    }

    /**
     * crop to limit
     */
    private void cropResult() {
        if(limitType == SuggestionRequestHandler.LimitType.each) {
            for(String field : fields.keySet()) {
                if(fields.get(field).size() > limit) {
                    fields.put(field, fields.get(field).subList(0,limit));
                }
            }
        } else {
            HashMap<String,List<Facet>> _f = new HashMap<String, List<Facet>>();
            boolean more = true;
            int number = 0;
            int c = 0;

            //long time = System.currentTimeMillis();

            while(c<limit && more) {
                more = false;
                for(String field : fields.keySet()) {
                    if(fields.get(field).size()>number) {
                        more = true;
                        c++;
                        if(!_f.containsKey(field)) _f.put(field,new ArrayList<Facet>());
                        _f.get(field).add(fields.get(field).get(number));
                    }
                    if(c==limit) break;
                }
                number++;
            }

            //System.out.println("Time for ordering: "+(System.currentTimeMillis()-time));

            fields = _f;
        }
    }

    public void addFacet(String field, String value, int count, int position) {
        if(fields.get(field) == null) {
            fields.put(field,new ArrayList<>());
        }

        fields.get(field).add(new Facet(value,count,position));
    }

    public int getCount() {
        return fields.size();
    }

    class Facet implements Comparable<Facet>{

        int position;
        String value;
        int count;

        Facet(String value, int count, int position) {
            this.value = value;
            this.count = count;
            this.position = position;
        }

        @Override
        public int compareTo(Facet facet) {
            return position == facet.position ? Integer.compare(facet.count,count) : Integer.compare(position, facet.position);
        }
    }

}
