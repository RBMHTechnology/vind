## 13. Contextualized fields

Searchlib supports the definition of document fields which can have different values for different contexts, allowing to get
 
```java
final Document d1 = assets.createDoc("1")
        .setValue(numberField, 0f)
        .setContextualizedValue(numberField, "privateContext", 24f)
        .setContextualizedValue(numberField, "singleContext", 3f)
        .setValue(entityID, "123")
        .setValue(dateField, new Date());
```

The search context can be set when creating the search object by the method modifier _context("contextname")_:

```java
final FulltextSearch searchAll = Search.fulltext().context("numberContext").filter(and(eq(entityID, "123"), eq(numberField,24f))).facet(entityID);
```

Those values which belong to a different context thant the one defined will not be in the search result.

```java
final SearchResult searchResult = server.execute(searchAll, assets);
searchResult.getResults().get(0).getContextualizedValue(numberField, "numberContext"));
```
