## 10. Partial Updates

To be able to perform atomic updates in documents the DocumentFactory should be set as 
'updatable' by seting the proper flag to _true_ as described below: 

```java
DocumentFactory asset = new DocumentFactoryBuilder("asset")
     .setUpdatable(true)
     .addField(title, cat_multi, cat_single)
     .build();
```

Once this pre-requisite are fulfilled partial updates are straight forward using field descriptors.

```java
SingleValueFieldDescriptor<String> title = new FieldDescriptorBuilder()
        .setFullText(true)
        .buildTextField("title");

MultiValueFieldDescriptor.NumericFieldDescriptor<Long> cat_multi = new FieldDescriptorBuilder()
        .setFacet(true)
        .buildMultivaluedNumericField("category", Long.class);

SingleValueFieldDescriptor.NumericFieldDescriptor<Long> cat_single = new FieldDescriptorBuilder()
        .setFacet(true)
        .buildNumericField("category", Long.class);

server.execute(Search.update("123").set(title,"123").add(cat_multi,1L,2L).remove(cat_single));    
```
