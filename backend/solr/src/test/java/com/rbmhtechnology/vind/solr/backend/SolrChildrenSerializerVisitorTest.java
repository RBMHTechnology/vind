package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 20.09.17.
 */
public class SolrChildrenSerializerVisitorTest {

    private DocumentFactory parent, child;
    private SingleValueFieldDescriptor<String> parent_value;
    private SingleValueFieldDescriptor<String> child_value;
    private SingleValueFieldDescriptor<String> shared_value;

    @Before
    public void before() {

        parent_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("parent_value");

        child_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setSuggest(true)
                .setFullText(true)
                .buildTextField("child_value");

        shared_value = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("shared_value");

        parent = new DocumentFactoryBuilder("parent")
                .setUpdatable(true)
                .addField(shared_value, parent_value)
                .build();

        child = new DocumentFactoryBuilder("child")
                .setUpdatable(true)
                .addField(shared_value, child_value)
                .build();
    }

    @Test
    @Ignore
    public void testFilterSerialization() {

        final SolrChildrenSerializerVisitor serializer = new SolrChildrenSerializerVisitor(parent, child, null, false);

        final Filter.DescriptorFilter<String> parentFilter =
                new Filter.DescriptorFilter<>(parent_value, "p_v", Filter.Scope.Facet);
        final Filter.DescriptorFilter<String> sharedFilter =
                new Filter.DescriptorFilter<>(shared_value, "s_v", Filter.Scope.Facet);

        final Filter.AndFilter andFilter = new Filter.AndFilter(parentFilter, sharedFilter);

        final String serializedFilter = andFilter.accept(serializer);

        assertEquals("((_type_:parent AND dynamic_single_stored_facet_string_parent_value:\"p_v\") AND {!parent which='_type_:parent' v='_type_:child AND dynamic_single_stored_facet_string_shared_value:\"s_v\"'})",serializedFilter);

    }



}
