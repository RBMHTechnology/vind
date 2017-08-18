package com.rbmhtechnology.vind.utils.mam;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public interface MetadataProvider {

    Document getDocument(Document document, DocumentFactory factory) throws IOException;

    Collection<Document> getDocuments(Collection<Document> documents, DocumentFactory factory) throws IOException;

    Document getDocumentById(String id, DocumentFactory factory) throws IOException;

    Collection<Document> getDocumentsByIds(Collection<String> ids, DocumentFactory factory) throws IOException;

    <T> T getObject(String id, Class<T> t) throws IOException;

    <T> Collection<T> getObjects(Collection<String> ids, Class<T> t) throws IOException;

    <T> T getObject(T o) throws IOException;

    <T> Collection<T> getObjects(Collection<T> os) throws IOException;

}
