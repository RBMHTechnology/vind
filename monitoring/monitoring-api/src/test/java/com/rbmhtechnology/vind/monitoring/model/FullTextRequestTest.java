package com.rbmhtechnology.vind.monitoring.model;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.filter.Filter.AndFilter;
import com.rbmhtechnology.vind.monitoring.model.request.FullTextRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created on 04.10.17.
 */
public class FullTextRequestTest {

    @Mock
    private FulltextSearch search;

    @Mock
    private AndFilter andFilter;

    @Mock
    private TermFilter termFilter;

    @Mock
    private Map<String,Object> facets;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setup(){

        Set<Filter> filters = new HashSet<>(Arrays.asList(termFilter));
        when(search.getSearchString()).thenReturn("mocked Query");
        when(search.getFilter()).thenReturn(andFilter);
        when(andFilter.getChildren()).thenReturn(filters);
    }

    @Test
    public void testCreateFullTextRequest(){
        final FullTextRequest fullTextRequest = new FullTextRequest(search, "q=*.*");

        assertEquals("mocked Query", fullTextRequest.getQuery());
        assertEquals("andFilter", fullTextRequest.getFilter().toString());
    }

}
