package com.rbmhtechnology.vind.api;

import com.google.common.collect.ImmutableSet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 04.07.16.
 */
public class FilterTest {

    @Test
    public void testAndFilters() {

        Filter f = Filter.AndFilter.fromSet(ImmutableSet.of(eq("a", "1"),eq("b","2"),eq("b","3")));

        Assert.assertTrue(f instanceof Filter.AndFilter);

        Assert.assertEquals(3, ((Filter.AndFilter)f).getChildren().size());

        Set<String> values = ImmutableSet.of("a","b","c","d");

        Filter filter = values.stream().map(v -> eq("cat",v)).collect(Filter.AndCollector);

        Assert.assertTrue(filter instanceof Filter.AndFilter);

        Assert.assertEquals(4, ((Filter.AndFilter)filter).getChildren().size());
    }

}
