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

    //Vind #66
    @Test
    public void testNormalization() {

        final ChildrenFilterSerializer serializer = new ChildrenFilterSerializer(parent, child, null, false, false);

        Filter parentValueFilter = new OrFilter(eq(parent_value,"value1"), eq(parent_value,"value2"));
        Filter sharedValueFilter = new OrFilter(new OrFilter(eq(shared_value,"shared1"), eq(shared_value,"shared2")), eq(shared_value,"shared3"));

        Filter mainFilter = new AndFilter(parentValueFilter, sharedValueFilter);

        // Original
        //
        //                  AND
        //                 /   \
        //                OR    OR
        //              / | \   / \
        //            s1 s2 s3 v1 v2
        //============================
        // DNF
        //
        //                         OR
        //             /   /     |     |    \     \
        //          AND   AND   AND   AND   AND   AND
        //          / \   / \   / \   / \   / \   / \
        //         s1 v1 s1 v2 s2 v1 s2 v2 s3 v1 s3 v2

        Filter normalizedFilter = serializer.normalize(mainFilter);

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(6,((OrFilter)normalizedFilter).getChildren().size());

        // Original
        //
        //                      AND
        //                 /     |    \
        //                OR    AND   OR
        //              / | \   / \   / \
        //            s1 s2 s3 s4 c1 v1 v2
        //
        //
        //============================
        // DNF
        //
        //                                             OR
        //              /            /           |           |          \          \
        //             AND          AND         AND         AND         AND         AND
        //          / /  \ \    /  /  \ \    /  / \ \    /  / \ \    /  / \  \   /  / \  \
        //        s1  s4 c1 v1 s1 s4  c1 v2 s2 s4 c1 v1 s2 s4 c1 v2 s3 s4 c1 v1 s3 s4 c1 v2

        Filter parentChildrenValueFilter = new AndFilter(eq(shared_value,"shared4"), eq(child_value,"child1"));

        mainFilter = new AndFilter(new AndFilter(parentValueFilter, sharedValueFilter), parentChildrenValueFilter);
        normalizedFilter = serializer.normalize(mainFilter);

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(6,((OrFilter)normalizedFilter).getChildren().size());


        // Original
        //
        //                  AND
        //                 /   \
        //                OR    OR
        //              / | \   / \
        //            AND s2 s3 v1 v2
        //            / \
        //          s1  c1
        //============================
        // DNF
        //
        //                               OR
        //             /      /       |     |    \     \
        //           AND     AND     AND   AND   AND   AND
        //          / | \   / | \    / \   / \   / \   / \
        //        s1 c1 v1 s1 c1 v2 s2 v1 s2 v2 s3 v1 s3 v2


        parentChildrenValueFilter = new AndFilter(eq(shared_value,"shared1"), eq(child_value,"child1"));

        sharedValueFilter = new OrFilter(new OrFilter(parentChildrenValueFilter, eq(shared_value,"shared2")), eq(shared_value,"shared3"));

        mainFilter = new AndFilter(parentValueFilter, sharedValueFilter);
        normalizedFilter = serializer.normalize(mainFilter);

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(6,((OrFilter)normalizedFilter).getChildren().size());

        // Original
        //
        //                  OR
        //                 /   \
        //                OR    AND
        //              / | \   / \
        //            AND s2 s3 v1 v2
        //            / \
        //          s1  c1
        //============================
        // DNF
        //
        //                   OR
        //              /   /  \   \
        //            AND  s2  s3  AND
        //            / \          / \
        //           s1 c1        v1 v2

        parentChildrenValueFilter = new AndFilter(eq(shared_value,"shared1"), eq(child_value,"child1"));

        sharedValueFilter = new OrFilter(new OrFilter(parentChildrenValueFilter, eq(shared_value,"shared2")), eq(shared_value,"shared3"));

        mainFilter = new OrFilter(new AndFilter(eq(parent_value,"value1"), eq(parent_value,"value2")), sharedValueFilter);
        normalizedFilter = serializer.normalize(mainFilter);

        assertEquals("OrFilter",normalizedFilter.getType());
        assertEquals(4,((OrFilter)normalizedFilter).getChildren().size());
    }


}
