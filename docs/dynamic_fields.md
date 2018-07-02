## 4. Dynamic Fields

It is often useful to make item creation configurable on runtime. In this step we learn
 how to use a dynamic document configuration.

### 4.1 Dynamic Configuration

We create an DocumentFactoryBuilder with some fields (similar fields like the News Item in the former
steps) which is used to build a immutable DocumentFactory.The fields are used later for both indexing 
and searching. IMPORTANT: In the current status the fieldnames '\_type_' and '\_id_' are reserved words, do not use 
them for custom fields.

```java
private SingleValueFieldDescriptor.TextFieldDescriptor<String> title;
private SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created;
private MultiValueFieldDescriptor.TextFieldDescriptor<String> category;
private SingleValueFieldDescriptor.NumericFieldDescriptor<Integer> ranking;

private DocumentFactory newsItems;

public SearchService() {

    //a simple fulltext field named 'title'
    this.title = new FieldDescriptorBuilder()
            .setFullText(true)
            .buildTextField("title");

    //a single value date field
    this.created = new FieldDescriptorBuilder()
            .buildDateField("created");

    //a multivalue text field used for fulltext and facet.
    //we also add a boost
    this.category = new FieldDescriptorBuilder()
            .setFacet(true)
            .setFullText(true)
            .setBoost(1.2f)
            .buildMultivaluedTextField("category");

    this.ranking = new FieldDescriptorBuilder()
            .setFacet(true)
            .buildNumericField("ranking", Integer.class);

    //all fields are added to the document factory
    newsItems = new DocumentFactoryBuilder("newsItem")
            .addField(title, created, category, ranking)
            .build();
}
```
