package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.util.FunctionHelpers;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.*;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.Serializable;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 */
public class AnnotationUtilTest {

    @Test
    public void testCreateDocument1() throws Exception {
        Pojo1 p = new Pojo1();
        p.id = "foo";
        p.title = "title";
        p.content = "content";
        p.someInternalData = UUID.randomUUID().toString();
        p.categories = new HashSet<>(Arrays.asList("cat1", "cat2", "cat3"));
        p.tax = new Taxonomy("id-1","term 1", Arrays.asList("first Term","term 1", "term one"));

        final Document doc = AnnotationUtil.createDocument(p);
        assertThat("id", doc.getId(), equalTo(p.id));
        assertThat("type", doc.getType(), equalTo("Pojo"));

        assertThat("title", doc.getValue("title"), allOf(instanceOf(String.class), CoreMatchers.<Object>equalTo(p.title)));
        assertThat("content", doc.getValue("data"), allOf(instanceOf(String.class), equalTo(p.content)));
        assertFalse("someInternalData", doc.hasValue("someInternalData"));

        assertThat("categories", doc.getValue("cats"), instanceOf(Collection.class));
    }

    @Test
    public void testCreatePojo1() throws Exception {

        FieldDescriptor t = new FieldDescriptorBuilder().buildTextField("title");
        FieldDescriptor c = new FieldDescriptorBuilder().buildTextField("data");
        FieldDescriptor cats = new FieldDescriptorBuilder().buildMultivaluedTextField("cats");
        FieldDescriptor tax = new ComplexFieldDescriptorBuilder<Taxonomy,String, String>().buildTextComplexField("tax", Taxonomy.class,String.class, String.class);

        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("Pojo");
        docFactoryBuilder.addField(t)
                        .addField(c)
                        .addField(cats)
                        .addField(tax);

        final DocumentFactory factory = docFactoryBuilder.build();
        final Document doc = factory.createDoc("foo")
                .setValue(t.getName(), "title")
                .setValue(c.getName(), "content")
                .setValues(cats.getName(), "cat1", "cat2", "cat3")
                .setValue(tax, new Taxonomy("id-2","term 2",Arrays.asList("term 2","second term")));


        final Pojo1 pojo = AnnotationUtil.createPojo(doc, Pojo1.class);

        assertThat("id", pojo.id, equalTo("foo"));

        assertThat("title", pojo.title, equalTo("title"));
        assertThat("content", pojo.content, equalTo("content"));

        assertThat("categories", pojo.categories, CoreMatchers.<Collection<String>>allOf(hasSize(3), containsInAnyOrder("cat1", "cat2", "cat3")));

    }

    @Test
    public void testPojo2RoundTrip() {
        Pojo2 p2 = new Pojo2();
        p2.id = UUID.randomUUID().toString();
        p2.title = "Title";
        p2.someInternalData = UUID.randomUUID().toString();
        p2.content = "Content";
        p2.categories = new HashSet<>(Arrays.asList("cat1", "cat2", "cat3"));
        p2.counter = 17;
        p2.tax = new Taxonomy("id-3","term 3",Arrays.asList("term 3","third term"));

        final Document doc = AnnotationUtil.createDocument(p2);

        assertThat("doc.id", doc.getId(), is(p2.id));
        assertThat("doc.type", doc.getType(), is(p2.getClass().getSimpleName()));

        assertThat("doc.field(title)", doc.getValue("title"), allOf(instanceOf(String.class), is(p2.title)));
        assertThat("doc.field(content)", doc.getValue("data"), allOf(instanceOf(String.class), is(p2.content)));
        assertThat("doc.field(counter)", doc.getValue("counter"), allOf(instanceOf(int.class), is(p2.counter)));
        assertThat("doc.filed(categories)", doc.getValue("cats"), instanceOf(Collection.class));

        final Pojo2 pojo = AnnotationUtil.createPojo(doc, Pojo2.class);

        assertThat("pojo.id", pojo.id, is(p2.id));
        assertThat("pojo.title", pojo.title, is(p2.title));
        assertThat("pojo.content", pojo.content, is(p2.content));
        assertThat("pojo.counter", pojo.counter, is(p2.counter));
        assertThat("pojo.categories", pojo.categories, hasSize(3));
        assertThat("pojo.categories", pojo.categories, CoreMatchers.<Collection<String>>allOf(hasSize(3), containsInAnyOrder(p2.categories.toArray())));
        assertThat("pojo.someInternalData", pojo.someInternalData, nullValue());
    }


    @Type(name = "Pojo")
    @SuppressWarnings("unused")
    public static class Pojo1 {

        @Id
        protected String id;

        protected String title;

        @Ignore
        protected String someInternalData;

        @Field(name = "data")
        protected String content;

        @Field(name = "cats")
        protected HashSet<String> categories;

        @ComplexField(  store = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "term"),
                        facet = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "term"),
                        suggestion = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "synonyms"),
                        advanceFilter = @Operator( function = FunctionHelpers.GetterFunction.class, fieldName = "id"))
        protected Taxonomy tax;

    }

    public static class Pojo2 extends Pojo1 {

        protected int counter;

    }

    public static class Taxonomy implements Serializable {
        public String id;
        public String term;
        public List<String> synonyms;

        public Taxonomy(String id, String term, List<String> synonyms) {
            this.id = id;
            this.term = term;
            this.synonyms = synonyms;
        }

        public String getId() {
            return id;
        }

        public String getTerm() {
            return term;
        }

        public List<String> getSynonyms() {
            return synonyms;
        }

    }
}