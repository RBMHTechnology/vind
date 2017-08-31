## 11. Nested Documents

Nested documents are supported by allowing to add documents as child fields of other documents. It is a 
fixed restriction that the nested document type is not the same as the parent document, so each one has to 
have defined their own DocumentFactory with a unique name.

The nested documents are added to the parent document as described below:

```java
SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
        .setFullText(true)
        .buildTextField("title");

SingleValueFieldDescriptor<String> color = new FieldDescriptorBuilder()
        .setFullText(true)
        .buildTextField("color");

DocumentFactory marker = new DocumentFactoryBuilder("marker")
        .addField(title, color)
        .build();

DocumentFactory asset = new DocumentFactoryBuilder("asset")
        .addField(title, color)
        .build();

Document a1 = asset.createDoc("A1")
        .setValue(title,"A1")
        .setValue(color,"blue");
        
Document a2 = asset.createDoc("A2")
        .setValue(title, "A2")
        .setValue(color,"red")
        .addChild(marker.createDoc("C1")
                .setValue(title, "C1")
                .setValue(color,"blue")
        );
```

After indexing and commit the parents, the nested document will be added to the index as any other 
document but keeping an internal relation with the parent. Therefore it is possible to perform 
search by types in both, parents or children documents. 

```java
server.execute(Search.fulltext("some"), asset); //search assets
server.execute(Search.fulltext("some"), marker); //search markers
```

Having nested documents allows to perform complex queries on them, excluding or including results 
based on matching children. If a fulltext search is not specified, the same query and filters used 
for the parent documents will be used for matching the child documents.

```java
//Search for those assets with color blue in either the asset or the marker. 
SearchResult orChildrenFilteredSearch = server.execute(
    Search.fulltext()
        .filter(Filter.eq(color, "blue"))
        .orChildrenSearch(marker),
     asset);

//Search for those assets with color blue in both the asset and the marker.
SearchResult andChildrenFilteredSearch = server.execute(
    Search.fulltext()
        .filter(Filter.eq(color, "blue"))
        .andChildrenSearch(marker), 
    asset); //search in all markers

//First filter the assets with color blue or a marker with color red, then search those assets with
//a '1' in fulltext field or any marker. 
SearchResult orChildrenCustomSearch = server.execute(
        Search.fulltext("1")
            .filter(Filter.eq(color, "blue"))
            .orChildrenSearch(
                Search.fulltext()
                    .filter(Filter.eq(color, "red")),
                marker), 
        asset); //search in all markers
```