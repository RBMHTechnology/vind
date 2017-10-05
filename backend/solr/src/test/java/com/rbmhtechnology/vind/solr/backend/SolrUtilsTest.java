package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.solr.backend.SolrUtils;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 22.06.16.
 */
public class SolrUtilsTest {

    @Test
    public void testFieldNameConfiguration() {

        FieldDescriptor facet = new FieldDescriptorBuilder().buildMultivaluedNumericField("test1");
        assertEquals("dynamic_multi_float_test1", SolrUtils.Fieldname.getFieldname(facet, SolrUtils.Fieldname.UseCase.Stored, null));
        assertEquals(2, SolrUtils.Fieldname.getFieldnames(facet, null).size());
        assertThat(SolrUtils.Fieldname.getFieldnames(facet, null),containsInAnyOrder("dynamic_multi_float_test1","dynamic_single_sort_float_test1"));
        
        FieldDescriptor stored2 = new FieldDescriptorBuilder().setFacet(true).buildMultivaluedNumericField("test3", Integer.class);
        assertEquals("dynamic_multi_facet_int_test3", SolrUtils.Fieldname.getFieldname(stored2, SolrUtils.Fieldname.UseCase.Facet, null));
        assertEquals(3, SolrUtils.Fieldname.getFieldnames(stored2,null).size());
        assertThat(SolrUtils.Fieldname.getFieldnames(stored2,null),containsInAnyOrder("dynamic_multi_int_test3","dynamic_multi_facet_int_test3","dynamic_single_sort_int_test3"));

        FieldDescriptor mixed = new FieldDescriptorBuilder().setFullText(true).setFacet(true).buildMultivaluedTextField("test4");
        assertEquals("dynamic_multi_facet_string_test4", SolrUtils.Fieldname.getFieldname(mixed, SolrUtils.Fieldname.UseCase.Facet, null));
        assertEquals("dynamic_multi_string_test4", SolrUtils.Fieldname.getFieldname(mixed, SolrUtils.Fieldname.UseCase.Stored, null));
        assertEquals("dynamic_multi_none_test4", SolrUtils.Fieldname.getFieldname(mixed, SolrUtils.Fieldname.UseCase.Fulltext, null));
        assertEquals(4, SolrUtils.Fieldname.getFieldnames(mixed, null).size());
        assertThat(SolrUtils.Fieldname.getFieldnames(mixed, null),containsInAnyOrder("dynamic_single_sort_string_test4","dynamic_multi_string_test4","dynamic_multi_none_test4","dynamic_multi_facet_string_test4"));

    }

}