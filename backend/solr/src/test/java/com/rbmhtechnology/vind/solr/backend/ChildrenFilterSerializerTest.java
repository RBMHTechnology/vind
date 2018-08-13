package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @since 20.09.17.
 */
public class ChildrenFilterSerializerTest {

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

        final ChildrenFilterSerializer serializer = new ChildrenFilterSerializer(parent, child, null, false, true);

        final Filter.DescriptorFilter<String> parentFilter =
                new Filter.DescriptorFilter<>(parent_value, "p_v", Scope.Facet);
        final Filter.DescriptorFilter<String> sharedFilter =
                new Filter.DescriptorFilter<>(shared_value, "s_v", Scope.Facet);
        final Filter.DescriptorFilter<String> childrenFilter =
                new Filter.DescriptorFilter<>(child_value, "c_v", Scope.Facet);

        final AndFilter andFilter = new AndFilter(parentFilter, sharedFilter);

        String serializedFilter = serializer.serialize(andFilter);

        assertEquals("_type_:parent AND dynamic_single_stored_facet_string_parent_value:\"p_v\" AND {!parent which='_type_:parent' v='_type_:child AND dynamic_single_stored_facet_string_shared_value:\"s_v\"'}",serializedFilter);

        serializedFilter = serializer.serialize(new AndFilter(new OrFilter(childrenFilter, andFilter),not(childrenFilter)));

        assertEquals("({!parent which='_type_:parent' v='_type_:child AND NOT(dynamic_single_stored_facet_string_child_value:\"c_v\") AND dynamic_single_stored_facet_string_child_value:\"c_v\"'} ) OR (_type_:parent AND dynamic_single_stored_facet_string_parent_value:\"p_v\" AND {!parent which='_type_:parent' v='_type_:child AND NOT(dynamic_single_stored_facet_string_child_value:\"c_v\") AND dynamic_single_stored_facet_string_shared_value:\"s_v\"'} )",serializedFilter);
    }

    @Test
    public void testFilterNormalization() {

        final ChildrenFilterSerializer serializer = new ChildrenFilterSerializer(parent, child, null, false, true);

         final Filter.DescriptorFilter<String> parentFilter =
                new Filter.DescriptorFilter<>(parent_value, "p_v", Scope.Facet);
         final Filter.DescriptorFilter<String> sharedFilter =
                new Filter.DescriptorFilter<>(shared_value, "s_v", Scope.Facet);

         AndFilter andFilter = new AndFilter(parentFilter, sharedFilter);

         Filter normalizedFilter = serializer.normalize(andFilter);

        assertEquals("AndFilter",normalizedFilter.getType());
        assertEquals(2,((AndFilter)normalizedFilter).getChildren().size());


        final NotFilter doubleNegationFilter =
                new NotFilter(new NotFilter(new Filter.DescriptorFilter<>(shared_value, "s_v", Scope.Facet)));

        andFilter = new AndFilter(parentFilter, doubleNegationFilter);

        normalizedFilter = serializer.normalize(andFilter);

        assertEquals("AndFilter",normalizedFilter.getType());
        assertEquals(2,((AndFilter)normalizedFilter).getChildren().size());


        normalizedFilter = serializer.normalize(not(andFilter));

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(2,((OrFilter)normalizedFilter).getChildren().size());


        normalizedFilter = serializer.normalize(new AndFilter(parentFilter,not(andFilter)));

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(2,((OrFilter)normalizedFilter).getChildren().size());

        normalizedFilter = serializer.normalize(new AndFilter(new OrFilter(parentFilter, andFilter),not(parentFilter)));

        final String serializedFilter = serializer.serialize(new AndFilter(new OrFilter(parentFilter, andFilter),not(parentFilter)));

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(2,((OrFilter)normalizedFilter).getChildren().size());
    }



}
