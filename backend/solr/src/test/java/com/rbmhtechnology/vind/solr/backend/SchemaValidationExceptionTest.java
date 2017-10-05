package com.rbmhtechnology.vind.solr.backend;

import com.google.common.io.Resources;
import com.rbmhtechnology.vind.solr.backend.SchemaValidationException;
import com.rbmhtechnology.vind.solr.backend.SolrSchemaChecker;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by fonso on 7/26/16.
 */
public class SchemaValidationExceptionTest {

    private static final String TEST_SOLR_SCHEMA = "schema.xml";
    private Path solrSchemaPath;

    @Mock
    private SchemaResponse schemaResponse;

    @Mock
    private SchemaRepresentation schemaRepresentation;

    @Mock
    private FieldTypeDefinition stringFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition booleanFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition intFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition floatFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition longFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition doubleFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition dateFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition textGeneralFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition textDeFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition textEnFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition textEsFieldTypeDefinition;

    @Mock
    private FieldTypeDefinition pathFieldTypeDefinition;

    @Before
    public void init() throws IOException, SolrServerException, URISyntaxException {
        solrSchemaPath = Paths.get(Resources.getResource(TEST_SOLR_SCHEMA).toURI());

        MockitoAnnotations.initMocks(this);

        Map<String, Object> stringFieldTypeAttributes = new HashMap<>();
        stringFieldTypeAttributes.put("name","string");
        when(stringFieldTypeDefinition.getAttributes()).thenReturn(stringFieldTypeAttributes);

        Map<String, Object> booleanFieldTypeAttributes = new HashMap<>();
        booleanFieldTypeAttributes.put("name","boolean");
        when(booleanFieldTypeDefinition.getAttributes()).thenReturn(booleanFieldTypeAttributes);

        Map<String, Object> intFieldTypeAttributes = new HashMap<>();
        intFieldTypeAttributes.put("name","int");
        when(intFieldTypeDefinition.getAttributes()).thenReturn(intFieldTypeAttributes);

        Map<String, Object> floatFieldTypeAttributes = new HashMap<>();
        floatFieldTypeAttributes.put("name","float");
        when(floatFieldTypeDefinition.getAttributes()).thenReturn(floatFieldTypeAttributes);

        Map<String, Object> longFieldTypeAttributes = new HashMap<>();
        longFieldTypeAttributes.put("name","long");
        when(longFieldTypeDefinition.getAttributes()).thenReturn(longFieldTypeAttributes);

        Map<String, Object> doubleFieldTypeAttributes = new HashMap<>();
        doubleFieldTypeAttributes.put("name","double");
        when(doubleFieldTypeDefinition.getAttributes()).thenReturn(doubleFieldTypeAttributes);

        Map<String, Object> dateFieldTypeAttributes = new HashMap<>();
        dateFieldTypeAttributes.put("name","date");
        when(dateFieldTypeDefinition.getAttributes()).thenReturn(dateFieldTypeAttributes);

        Map<String, Object> textGeneralFieldTypeAttributes = new HashMap<>();
        textGeneralFieldTypeAttributes.put("name","text_general");
        when(textGeneralFieldTypeDefinition.getAttributes()).thenReturn(textGeneralFieldTypeAttributes);

        Map<String, Object> textDeFieldTypeAttributes = new HashMap<>();
        textDeFieldTypeAttributes.put("name","text_de");
        when(textDeFieldTypeDefinition.getAttributes()).thenReturn(textDeFieldTypeAttributes);

        Map<String, Object> textEnFieldTypeAttributes = new HashMap<>();
        textEnFieldTypeAttributes.put("name","text_en");
        when(textEnFieldTypeDefinition.getAttributes()).thenReturn(textEnFieldTypeAttributes);

        Map<String, Object> textEsFieldTypeAttributes = new HashMap<>();
        textEsFieldTypeAttributes.put("name","text_es");
        when(textEsFieldTypeDefinition.getAttributes()).thenReturn(textEsFieldTypeAttributes);

        Map<String, Object> pathFieldTypeAttributes = new HashMap<>();
        pathFieldTypeAttributes.put("name","path");
        when(pathFieldTypeDefinition.getAttributes()).thenReturn(pathFieldTypeAttributes);

        List<FieldTypeDefinition> fieldTypeDefinitions = new ArrayList<>();
        fieldTypeDefinitions.add(stringFieldTypeDefinition);
        fieldTypeDefinitions.add(booleanFieldTypeDefinition);
        fieldTypeDefinitions.add(intFieldTypeDefinition);
        fieldTypeDefinitions.add(floatFieldTypeDefinition);
        fieldTypeDefinitions.add(longFieldTypeDefinition);
        fieldTypeDefinitions.add(doubleFieldTypeDefinition);
        fieldTypeDefinitions.add(dateFieldTypeDefinition);
        fieldTypeDefinitions.add(textGeneralFieldTypeDefinition);
        fieldTypeDefinitions.add(textDeFieldTypeDefinition);
        fieldTypeDefinitions.add(textEnFieldTypeDefinition);
        fieldTypeDefinitions.add(textEsFieldTypeDefinition);
        fieldTypeDefinitions.add(pathFieldTypeDefinition);
        when(schemaRepresentation.getFieldTypes()).thenReturn(fieldTypeDefinitions);

        when(schemaResponse.getSchemaRepresentation()).thenReturn(schemaRepresentation);
    }
    @Test
    public void checkSchemaTest() throws IOException, SchemaValidationException {
        SolrSchemaChecker.checkSchema(solrSchemaPath,schemaResponse);
    }

}
