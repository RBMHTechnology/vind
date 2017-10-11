## 16. Extension: Utils

**This is a work is in an alpha state**

In order to support developers we provide some Utilities.

### 16.1 MAM Metadata Provider

The MAM Metadata Provider allows to *fill* properties of a Pojo by MAM field id.
To use it you have to add the dependency:
```xml
<dependency>
    <groupId>com.rbmhtechnology.vind</groupId>
    <artifactId>mam-utils</artifactId>
    <version>${vind.version}</version>
</dependency>
```

For Pojos you can specify the ID of the mam property using the metadata field.

```java
@FullText
@Metadata(@Entry(name = RESTMetadataProvider.ID, value = "1319102420792-686346531"))
public String title;

@Metadata(@Entry(name = RESTMetadataProvider.ID, value = "1404728958802-98806344"))
public ZonedDateTime created;
```

```java
MetadataProvider p = new RESTMetadataProvider(
        "https://mediamanager-staging.redbullmediahouse.com",
        "rbmh",
        "admin",
        "global",
        "1315204862832-1123067022",
        "asset",
        "developer",
        "developer"
);

//create object
Asset a = p.getObject("1359078847993-766700833",Asset.class);

assert a.getTitle().equals("Sean Pettit - Portrait");

//set values of an object
Asset a2 = p.getObject(new Asset("1359078847993-766700833"));

assert a2.getTitle().equals("Sean Pettit - Portrait");
```

For field descriptors this works analogously.

```java
//create descriptor and add metadata
SingleValueFieldDescriptor.TextFieldDescriptor<String> title = new FieldDescriptorBuilder()
    .setFullText(true)
    .setFacet(true)
    .putMetadata(RESTMetadataProvider.ID, "1319102420792-686346531")
    .buildTextField("title");
    
//create document
Document document = factory.createDoc("1359078847993-766700833");

//'fill' document
document = metadataProvider.getDocument(document, factory);
```