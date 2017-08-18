package com.rbmhtechnology.vind.utils.mam;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class RESTMetadataProviderTest {

    @Test
    @Ignore //ignored because of the username, password
    public void testGetDocument() throws Exception {
        MetadataProvider p = new RESTMetadataProvider(
                "https://mediamanager-staging.redbullmediahouse.com",
                "rbmh",
                "admin",
                "global",
                "1315204862832-1123067022",
                "asset",
                "user",
                "passw"
        );

        Asset a = p.getObject("1359078847993-766700833",Asset.class);
        Assert.assertEquals("Sean Pettit - Portrait", a.getTitle());

        Asset a2 = p.getObject(new Asset("1359078847993-766700833"));
        Assert.assertEquals("Sean Pettit - Portrait", a2.getTitle());
    }

    @Test
    @Ignore //ignored because of the username, password
    public void testGetDocument2() throws IOException {

        MetadataProvider metadataProvider = new RESTMetadataProvider(
                "https://mediamanager-staging.redbullmediahouse.com",
                "rbmh",
                "admin",
                "global",
                "1315204862832-1123067022",
                "asset",
                "user",
                "passw"
        );

        SingleValueFieldDescriptor.TextFieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true) //notwendig für suggestion, da wirds noch ein setSuggestion(bool) geben
                .putMetadata(RESTMetadataProvider.ID, "1319102420792-686346531")
                .buildTextField("title");

        SingleValueFieldDescriptor.TextFieldDescriptor<String> description = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true) //notwendig für suggestion, da wirds noch ein setSuggestion(bool) geben
                .putMetadata(RESTMetadataProvider.ID, "1315204278582-8008411")
                .buildTextField("description");

        DocumentFactory factory = new DocumentFactoryBuilder("asset")
                .addField(title, description)
                .build();

        Document document = factory.createDoc("1359078847993-766700833");

        document = metadataProvider.getDocument(document, factory);

        Assert.assertEquals("Sean Pettit - Portrait", document.getValue(title));
    }
}