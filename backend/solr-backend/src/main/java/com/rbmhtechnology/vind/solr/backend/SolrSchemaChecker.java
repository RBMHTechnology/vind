package com.rbmhtechnology.vind.solr.backend;

import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.response.schema.SchemaRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 14.07.16.
 */
public class SolrSchemaChecker {

    private static Logger log = LoggerFactory.getLogger(SolrSchemaChecker.class);

    public static void checkSchema(Path solrSchemaPath, SchemaResponse response) throws IOException, SchemaValidationException {
        // read the local schema.xml
        final Document local;
        try (InputStream xml = Files.newInputStream(solrSchemaPath, StandardOpenOption.READ)) {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            final DocumentBuilder builder = factory.newDocumentBuilder();

            local = builder.parse(xml);
        } catch (ParserConfigurationException | SAXException e) {
            log.error("Error checking schema.xml: {}", e.getMessage(), e);
            throw new IOException(e);
        }

        final SchemaRepresentation remote = response.getSchemaRepresentation();
        final Element schema = local.getDocumentElement();
        // check the field-types
        final NodeList fieldTypes = schema.getElementsByTagName("fieldType");
        final Set<String> fieldTypeNames = remote.getFieldTypes().stream()
                .map(FieldTypeDefinition::getAttributes)
                .map(m -> m.get("name"))
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toSet());
        for (int i = 0; i < fieldTypes.getLength(); i++) {
            final Node fieldType = fieldTypes.item(i);
            final String fieldTypeName = fieldType.getAttributes().getNamedItem("name").getNodeValue();
            if (! fieldTypeNames.contains(fieldTypeName)) {
                throw new SchemaValidationException(String.format("Missing <fieldType name='%s' />", fieldTypeName));
            }
        }

        // TODO: check local -> remote.

    }

}
