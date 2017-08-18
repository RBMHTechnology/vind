package com.rbmhtechnology.vind.utils.mam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class RESTMetadataProvider implements MetadataProvider {

    public static final String ID = "mam.id";

    private static final Logger LOG = LoggerFactory.getLogger(RESTMetadataProvider.class);

    private static final String FIELD_SET_DATA_PATH = "/api/metadata/v1/%s/%s/%s/fieldsets/%s/data/%s/";

    private final String baseURL;
    private final String path;

    private final String bearerToken;

    private final HttpClient client;

    private final ObjectMapper mapper;

    public RESTMetadataProvider(String baseURL,
                                String tnt,
                                String app,
                                String ws,
                                String fs,
                                String type,
                                String bearerToken) {
        this.baseURL = baseURL;
        this.bearerToken = bearerToken;
        this.path = String.format(FIELD_SET_DATA_PATH,tnt,app,ws,fs,type);
        this.client = new HttpClient();
        this.mapper = new ObjectMapper();

    }

    public RESTMetadataProvider(String baseURL,
                                     String tnt,
                                     String app,
                                     String ws,
                                     String fs,
                                     String type,
                                     String username,
                                     String password) {
        this.baseURL = baseURL;
        this.bearerToken = username+":"+password;
        this.path = String.format(FIELD_SET_DATA_PATH,tnt,app,ws,fs,type);
        this.client = new HttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Collection<Document> getDocuments(Collection<Document> documents, DocumentFactory factory) throws IOException {
        Collection<Document> c = new HashSet<>();
        for(Document doc : documents) {
            c.add(this.getDocument(doc, factory));
        }
        return c;
    }

    @Override
    public Document getDocument(Document document, DocumentFactory factory) throws IOException {

        HttpMethod request = new GetMethod(baseURL);

        request.addRequestHeader("Authorization", String.format("Bearer %s", bearerToken)); //TODO
        request.setPath(path + document.getId());

        int status = client.executeMethod(request);

        if(status == 200) {

            JsonNode json = mapper.readValue(request.getResponseBody(), JsonNode.class);

            for(FieldDescriptor descriptor : factory.listFields()) {

                try {
                    Object value = getValue(json, descriptor);
                    if(value != null) {
                        document.setValue(descriptor, value);
                    } else LOG.warn("No data found for id {}", document.getId());
                } catch (IOException e) {
                    LOG.warn("Cannot use data for id {}: {}", document.getId(), e.getMessage());
                }
            }

            return document;

        } else throw new IOException(request.getStatusText());
    }

    @Override
    public Document getDocumentById(String id, DocumentFactory factory) throws IOException {
        Document document = factory.createDoc(id);
        return getDocument(document, factory);
    }

    private Object getValue(JsonNode json, FieldDescriptor descriptor) throws IOException {
        if(descriptor.getMetadata().containsKey(ID)) {

            JsonNode node = json.get("data").get((String)descriptor.getMetadata().get(ID));

            if(node != null) {
                System.out.print(node);
                if(node.has("stringValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return node.get("stringValue").asText();

                } else if(node.has("stringValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<String> c = new ArrayList<>();
                    for (final JsonNode n : node.get("stringValues")) {
                        c.add(n.asText());
                    }
                    return c;

                } else if(node.has("dateValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return ZonedDateTime.parse(node.get("dateValue").asText());

                } else if(node.has("dateValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<ZonedDateTime> c = new ArrayList<>();
                    for (final JsonNode n : node.get("stringValues")) {
                        c.add(ZonedDateTime.parse(n.asText()));
                    }
                    return c;

                } else if(node.has("integerValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return node.get("integerValue").asInt();

                } else if(node.has("integerValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<Integer> c = new ArrayList<>();
                    for (final JsonNode n : node.get("integerValues")) {
                        c.add(n.asInt());
                    }
                    return c;

                } else if(node.has("floatValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return node.get("floatValue").asInt();

                } else if(node.has("floatValues")) { //TODO
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<Double> c = new ArrayList<>();
                    for (final JsonNode n : node.get("floatValues")) {
                        c.add(n.asDouble());
                    }
                    return c;

                } else if(node.has("longValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return node.get("longValue").asLong();

                } else if(node.has("longValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<Long> c = new ArrayList<>();
                    for (final JsonNode n : node.get("longValues")) {
                        c.add(n.asLong());
                    }
                    return c;

                } else if(node.has("booleanValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return node.get("booleanValue").asBoolean();

                } else if(node.has("booleanValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<Boolean> c = new ArrayList<>();
                    for (final JsonNode n : node.get("booleanValues")) {
                        c.add(n.asBoolean());
                    }
                    return c;

                } else if(node.has("choiceValue")) {
                    if(descriptor.isMultiValue()) throw new RuntimeException();
                    return toChoiceValue(node.get("choiceValue"));

                } else if(node.has("choiceValues")) {
                    if(!descriptor.isMultiValue()) throw new RuntimeException();
                    Collection<String> c = new ArrayList<>();
                    for (final JsonNode n : node.get("choiceValues")) {
                        c.add(toChoiceValue(n));
                    }
                    return c;
                } else {
                    throw new IOException("Field type not supported");
                }
            }
            return null;

        } else return null;
    }

    private String toChoiceValue(JsonNode node) { //TODO does not work as expected
        List<String> values = new ArrayList<>();

        for (Iterator<JsonNode> it = node.get("_meta").get("links").get("path").elements(); it.hasNext();) {
            JsonNode pathNode = it.next();
            values.add(pathNode.get("value").asText());//TODO what about the id??
        }

        return Joiner.on(" | ").join(values);
    }

    @Override
    public Collection<Document> getDocumentsByIds(Collection<String> ids, DocumentFactory factory) throws IOException {
        Collection<Document> c = new HashSet<>();
        for(String id : ids) {
            c.add(this.getDocumentById(id, factory));
        }
        return c;
    }

    @Override
    public <T> T getObject(String id, Class<T> t) throws IOException {
        return AnnotationUtil.createPojo(getDocumentById(id, AnnotationUtil.createDocumentFactory(t)),t);
    }

    @Override
    public <T> Collection<T> getObjects(Collection<String> ids, Class<T> t) throws IOException {
        Collection<T> c = new HashSet<>();
        for(String id : ids) {
            c.add(this.getObject(id, t));
        }
        return c;
    }

    @Override
    public <T> T getObject(T o) throws IOException {
        DocumentFactory factory = AnnotationUtil.createDocumentFactory(o.getClass());
        Document doc = AnnotationUtil.createDocument(o);
        return AnnotationUtil.createPojo(getDocumentById(doc.getId(), factory), (Class<T>) o.getClass());
    }

    @Override
    public <T> Collection<T> getObjects(Collection<T> os) throws IOException {
        Collection<T> c = new HashSet<>();
        for(T o : os) {
            c.add(getObject(o));
        }
        return c;
    }
}
