package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.api.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collection;

/**
 */
public class DocumentFactoryTest {

    private DocumentFactory factory;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {

        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("typeTest");


        FieldDescriptor<String> multipleStringField = new MultiValueFieldDescriptor.TextFieldDescriptor<>("multipleStringField", String.class);
        multipleStringField.setMultiValue(true);
        docFactoryBuilder.addField(multipleStringField);

        FieldDescriptor<String> singleStringField = new SingleValueFieldDescriptor.TextFieldDescriptor<>("singleStringField", String.class);
        singleStringField.setMultiValue(false);
        docFactoryBuilder.addField(singleStringField);

        factory = docFactoryBuilder.build();
    }

    @Test
    public void setValuesTest() {

        Document doc = factory.createDoc("idTest");
        doc.setValues("multipleStringField","1","2","3");
        Collection<Object> values = new ArrayList<>();
        values.add("4");
        values.add("5");
        doc.setValues("multipleStringField", values);

        Collection<Object> invalidValues = new ArrayList<>();
        invalidValues.add("6");
        invalidValues.add(7);

        exception.expect(IllegalArgumentException.class);
        doc.setValues("multipleStringField", invalidValues);
        doc.setValues("multipleStringField",1);
    }

    @Test
    public void addValueTest() {

        Document doc = factory.createDoc("idTest");
        doc.addValue("multipleStringField", "0");
        doc.addValue("multipleStringField", "4");

        exception.expect(IllegalArgumentException.class);
        doc.addValue("singleStringField", "5");
        doc.addValue("multipleStringField", 4);

    }

    @Test
    public void removeValueTest() {

        Document doc = factory.createDoc("idTest");
        doc.setValues("multipleStringField","1","2","3");
        doc.removeValue("multipleStringField","1");

        exception.expect(IllegalArgumentException.class);
        doc.removeValue("multipleStringField",1);

    }

    @Test
    public void getValueTest() {

        Document doc = factory.createDoc("idTest");
        doc.setValues("multipleStringField","1","2","3");
        doc.setValue("singleStringField", "4");

        Assert.assertEquals("get single value", doc.getValue("singleStringField"),"4");

        exception.expect(IllegalArgumentException.class);
        doc.getValue("imaginaryField");

    }
}