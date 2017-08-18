Introduction
============

This repository contains the modular redbull search-library for java. 
The lib was build to enable the integration of search facilities in several projects without getting to deep into the 
search topic. It should help programmers to come to a good solution in an assessable amount of time, improve the 
maintainability of software projects, and simplify a centralized search service management including monitoring and reporting.
 
The work tried to consider the following design issues:

**1. Versatility:** Vind will be used in many different projects, so it was an aim to keeping the dependency footprint small, 
which avoids version-clashes in the downstream projects.

**2. Backend Agnostic:** Wherever possible and feasible, the library has to abstracted from the basic search framework. This enabled us to change the
backend without migrating application software.

**3. Flat learning curve:** It was an aim to keep the learning curve rather flat, so we tried to use Java built-in constructs whenever possible. Additionally
we tried to follow the concept: easy things should be easy, complex things can (but does not have to) be complex.

The search lib is modular and implements the following layers:

![Search Lib Architecture](./demo/img/layer_cake.png)

More information is coming up soon at
https://charlie.redbullmediabase.com/display/SEARCHDEV/Specifications.

We built a short tutorial to give you a smooth entry to all the functions of the search lib.
The runnable code for each step can be found under `demo/demo-step{number}`.

Step 1: A simple Pojo search
===

Step one shows you, how quick and easy the search lib allows you to create a proper search on data items.

The dependency
---
The search lib is managed via maven/gradle repository. In our case we depend via maven dependency on the
embedded solr server. For production we will change this dependency to a remote solr server later.
```xml
<dependency>
    <groupId>com.rbmhtechnology.vind</groupId>
    <artifactId>embedded-solr-server</artifactId>
    <version>${vind.version}</version>
</dependency>
```

The pojo
---
Now we create a Pojo, which holds our data. To properly index it, we need at least an id-field.
This field has to be annotated with @Id. All the other annotations that we introduce are optional.

```java
@Id
private String id;

//the fulltext annotation means: 'use this for fulltext search'
@FullText
private String title;

//a field which is not annotated is just stored in the index
private ZonedDateTime created;
```

Create and Index
---
We instantiate a search server just by getting an instance. As mentioned it is an intance
 of an Embedded Solr Server. This server will loose all data when the program exit, so don't
 use it for production.

```java
//get an instance of a server (in this case a embedded solr server)
SearchServer server = SearchServer.getInstance();

//index 2 news items
server.indexBean(new NewsItem("1", "New Searchlib for Redbull needed", ZonedDateTime.now().minusMonths(3)));
server.indexBean(new NewsItem("2", "Redbull Searchlib available", ZonedDateTime.now()));

//don't forget to commit
server.commit();
```

Search and delete
---
Now we can retrieve the indexed documents via search. In addition we can delete existing News Items. After
index and/or delete, the action has to be persisted via commit or is persisted automatically within
5 seconds.

```java
//a first (empty) search, which should retrieve all News Items
BeanSearchResult<NewsItem> result = server.execute(Search.fulltext(), NewsItem.class);

//e voila, 2 news items are returned
assert result.getNumOfResults() == 2;

//delete an item
server.delete(i1);
server.commit();

//search again for all News Items
result = server.execute(Search.fulltext(), NewsItem.class);

//and we see, the item #1 is gone
assert result.getNumOfResults() == 1;
assert result.getResults().get(0).getId().equals("2");
```

Step 2: Fulltext, Facets and Score
===

This step shows extended annotations and gives on overview on search.

Extended Annotations
---

We extend the Pojo from Step 1 with some more values. Depending on what should be the role
of the field is annotated accordingly.

```java
//the field should be treated as english text
@FullText(language = Language.English)
private String title;

//we want to use this field for faceting and fulltext.
//additionally we want to boost the value for fulltext a bit (default is 1)
@FullText(language = Language.English, boost = 1.2f)
@Facet
private HashSet<String> category;

//this field is 'just' a facet field
@Facet
private String kind;

//we want to have a look at the search score (which is internally used for ranking)
//this field must be a float value and should not have a setter
@Score
private float score;
```

Search Building
---

No let's try it out. You can see, that the category field is used for fulltext search
and it influences the score more than the title. The other searches in the examples show,
how to use sorting and filtering and how to generate facet results.

```java
//this search should retrieve news items that should match the search term best
FulltextSearch search = Search.fulltext("redbull release");

BeanSearchResult<NewsItem> result = server.execute(search, NewsItem.class);

//now we want to have also the facets for category and kind.
//additionally we change the query
search.text("redbull");
search.facet("category","kind");

result = server.execute(search, NewsItem.class);

//new we define a search order based on the 'created ' field
search.sort(desc("created"));
result = server.execute(search, NewsItem.class);

//now we want to filter for all items with the kind 'blog'.
result = server.execute(Search.fulltext().filter(eq("kind","blog")), NewsItem.class);
```

Paging
------

Both the search and the result object supports paging.

```java
//this search should retrieve news items
//we set the page to 1 and the pagesize to 1
FulltextSearch search = Search.fulltext();
search.page(1,1);

BeanPageResult<NewsItem> result = server.execute(search, NewsItem.class);

//lets log the results
System.out.println("--- Page 1 ---");
result.getResults().forEach(System.out::println);
System.out.println();

//the result itself supports paging, so we can loop the pages
while(result.hasNextPage()) {
    result = result.nextPage();
    System.out.println("--- Page " + result.getPage() + " ---");
    result.getResults().forEach(System.out::println);
    System.out.println();
}
```

Slicing
-------

The search results can be also requested in the format of slices by specifying an offset and an
slice size.

```java
final FulltextSearch searchAll = Search.fulltext();
searchAll.slice(1, 10);

final BeanSearchResult<NewsItem> result = server.execute(search, NewsItem.class);
```

Suggestions
-----------

Suggestions suggest values based on free text. The suggestions also supports spellchecking 
(which is used automatically in the backend). If the term has to be spellchecked
to get suggestions, the collated spellchecked term is included in the result. Otherwise this term is `null`.

```java
SuggestionResult suggestions = server.execute(Search.suggest("c").field("category"), NewsItem.class);

//suggestions can be combined with filters
suggestion = server.execute(Search.suggest("c").field("title").filter(eq("kind","blog")), NewsItem.class));

//get spellchecked result
String spellcheckedQuery = suggestion.getSpellcheck();
```

*Note*: To query for suggestions on an specific field, this should have the suggest flag set
to true.

Step 3: Dynamic Fields
======================

It is often useful to make item creation configurable on runtime. In this step we learn
 how to use a dynamic document configuration.

Dynamic Configuration
---------------------

We create an DocumentFactoryBuilder with some fields (similar fields like the News Item in the former
steps) which is used to build a immutable DocumentFactory.The fields are used later for both indexing 
and searching. IMPORTANT: In the current status the fieldnames '\_type\_' and '\_id\_' are reserved words, do not use 
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

Indexing
--------

Now we can create news documents using the factory and the fields.
The setters are type-safe, and multivalued fields support also to set the fields with collections.

```java
Document document1 = newsItems.createDoc("1")
    .setValue(title, "New Searchlib for Redbull needed")
    .setValue(created, ZonedDateTime.now())
    .setValues(category, Arrays.asList("coding"))
    .setValue(ranking, 1);

Document document2 = newsItems.createDoc("2");
document.setValue(title, "Yet Another Document");
document.setValue(created, ZonedDateTime.now());
document.setValues(category, Arrays.asList("info"));
document.setValue(ranking, 1);

server.index(document1, document2);
server.commit();
```
_Note_: Remember to do a _commit_ after the index or no immediate changes will be seen in the solr server! 

Type-Save Search
----------------

The basic search is not much different to the one we already know except type
safety when using descriptors for filtering and sorting. Additionally the factory
allows to cast the search result fields in the right way.

```java
FulltextSearch search = Search.fulltext("redbull");

//ranking field is of type Integer, so only Integers are allowed here
search.filter(eq(ranking,1));

//for the execution we now use the factory as parameter
return server.execute(search, newsItems);
```

Step 4: Special Sorting, Filters and Facets
===========================================

In this step we show which kind of special filters, facets and sorting the searchlib provides.

Special Sorting
---------------

```java
//special sort filter allows to combine a date with scoring, so
//that best fitting and latest documents are ranked to top
search.sort(desc(scoredDate(created)));

//special sorting which gives results scored by distance to a 
//given location. The distance is meassured based on the 
//geoDistance defined for the search. 
search.sort(desc(distance()));
```

**Future Extensions:** Support more sortings

Type-aware filters
------------------

For numeric and date fields, special filters are supported.

```java
//between filter for datetime fields
search.filter(created.between(
                ZonedDateTime.now().minusDays(7),
                ZonedDateTime.now()
        )
);

//greater than filter for numeric fields
search.filter(ranking.greaterThan(3));
```

This is list of the type specific filters currently supported by the field descriptor syntax:

| Numeric  | Date | Text | Location | All |
|---|---|---|---|---|
| between  | between | equals | withinBBox | isEmpty |
| greaterThan | after | prefix | withinCircle | isNotEmpty |
| lesserThan | before |  |  | |

**Future Extensions:** Support more special filters 

Special Field Facets
--------------------

The library support several kind of facets. Facets have names, so they can be referenced in the result.
Do not use the same name for more than one facet (they will be overwritten). For names only alphanumeric chars are supported.

```java
//lets start with the range facet. It needs start, end and gap and is type aware.
search.facet(range("dates", created, ZonedDateTime.now().minus(Duration.ofDays(100)), ZonedDateTime.now(), Duration.ofDays(10)));

//query facets support the filters we already know from the queries, simple ones and complex
search.facet(query("middle", eq(category, 5L)));

search.facet(query("hotAndNew", and(category.between(7,10), created.after(ZonedDateTime.now().minus(Duration.ofDays(1))))))
     
//stats facet support statistics for facet field, like max, min, etc.
//it can be defined which stats should be returned, in this case count, sum and percentile
search.facet(stats("catStats", category).count().sum().percentiles(1,99,99.9));

//interval facets allows to perform faceting on a defined group of dates or numeric periodes; for each of those intervals it is needed to 
//provide a name the start value, the end value, if it is open (includes the value) or closed (does not include the value) on the start
//and on the end. If not specified both sides are open.
search.facet(
        interval("quality", marker,
                Interval.numericInterval("low", 0L, 2L, true, false),
                Interval.numericInterval("high", 3L, 4L)
        )
);

//pivot facets allows facetting on facets
search.facet(pivot("catsCreated", category, created));             
```

It is also supported by the library the combination of pivots with the other kind of facets.

**This is a work in progress**

```java
search.facet(pivot("catsNew", category, query("new",category.between(7,10)))); 

//if you want to use facets on the same level you can use lists
search.facet(pivot("catsNew", list(category, query("new",category.between(7,10))))); 
```

Step 5: An overall Example
==========================

This example uses the Spark micro framework and implements a search over guardian
news articles. Be aware the this is just for demo purposes and therefor very limited.
Don't index too many times because otherwise the demo api key expires ;) Just browse
the code or run the application and have fun ;)

API
---

HOST: http://localhost:4567/

* GET /index : Indexes 500 latest news
* GET /search : Simple search
    * q: The query string
    * filter: The category filter (combined by 'or') *(multivalue)*
* GET /news : Ranked by scored date
    * q: The query string
    * p: The page number
    * sort: score | date | scoredate
* GET /suggest
    * q: The query string 
   
Step 6: Use Solr Remote Backend
===============================

In this step we show how to use a Remote Solr Server as backend. Additionally we learn how
change basic configurations of the search lib.

Dependencies
------------

In order to use a Solr Remote Backend we have to switch the dependency to this.

```xml
<dependency>
    <groupId>com.rbmhtechnology.vind</groupId>
    <artifactId>remote-solr-server</artifactId>
    <version>${vind.version}</version>
</dependency>
```

Configuration
-------------

The searchlib uses property files for configuration. It comes with some basic configurations (like pagesize) that can be changed. 
 For configuring the remote host you have to place a file called searchlib.properties in the classpath which includes
 the host information.

```
//configure http client
server.solr.host=http://localhost:8983/solr/searchindex

//configure zookeeper client
server.solr.cloud=true
server.solr.host=zkServerA:2181,zkServerB:2181,zkServerC:2181
server.solr.collection=collection1

//change pagesize
search.result.pagesize=7
```
 
In addition to property file the static configuration interface allows also changes on runtime.
```java
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_HOST, "http://example.org/solr/core");
```
   
If you cannot (or do not want to) change the dependency on different profiles you can have also 2 server dependency on
classpath and select one by adding a configuration property. Currently 2 solr server provider (embedded and remote) are
supported.
```java
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_PROVIDER, "com.rbmhtechnology.vind.solr.EmbeddedSolrServerProvider");
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_PROVIDER, "com.rbmhtechnology.vind.solr.RemoteSolrServerProvider");
```

*Attention*: If you want to connect via zookeeper connection string, in addition to the host also the collection has to set.
```java
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_PROVIDER, "com.rbmhtechnology.vind.solr.RemoteSolrServerProvider");
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, true);
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_HOST, "zkServerA:2181,zkServerB:2181,zkServerC:2181");
SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_COLLECTION, "collection1");
```
   
*HINT*
If you want to test things with a standalone Solr Server we created a small script that helps you with that. The script is located
in the main directory of the project. Usage:

* `./solr_remote.sh start` downloads solr (if necessary), configures the server and starts it
* `./solr_remote.sh stop` stops the running server

   
Step 7: Completable Search Server
=================================

In some cases a non-blocking search server is useful. the Completable Search Server uses Java CompletableFuture 
and is implemented as a wrapper arround the existing search server. It can be instantiated
 with an Executor or uses (by default) a FixedThreadPool with 16 threads. This number is configurable via
 SearchConfiguration parameter `application.executor.threads`.
 
```java
CompletableSearchServer server = new CompletableSearchServer(SearchServer.getInstance());

CompletableFuture<SearchResult> resultFuture = server.executeAsync(Search.fulltext(),factory);
```
   
Step 8: Reporting
=================

**This is a work in beta state**

Reporting allows to log executed searches directly to a specific logger. The logger can be managed via
dependency and configured via search-configuration. The actual logger is loaded via classloader. In the example
we use the simple log reporter, which writes json logs using the slf4j logger.
```xml
<dependency>
    <groupId>com.rbmhtechnology.search</groupId>
    <artifactId>log-report-writer</artifactId>
    <version>${vind.version}</version>
</dependency>
```

The ReportingSearchServer is a wrapper over SearchServer and can be used with any backend.
It allows to set sessions so the log can be identified in a alter reporting process.

```java
Session session = new UserSession("213", "jdoe", "John Doe"); 
ReportingSearchServer reportingServer = new ReportingSearchServer(server, session);

//it can be used as a simple SearchServer
reportingServer.execute(Search.fulltext(),factory);
```
   
Step 9: Partial Updates
=======================
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


Step 10: Nested Documents
=========================
Nested documents are supported by allowing to add documents as child fields of other documents. It is a 
fixed restriction that the nested document type is not the same as the parent document, so each one has to 
have defined their own DocumentFactory.

The nested documents are added to the paren document as described below:

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

Step 11: Complex Fields
=======================

There are special situations in which having the same value for every scope (storing, fulltext search, filtering,
faceting, suggesting or sorting) may not be enough for the project requirements. Think of the scenario of a taxonomy 
term, with a unique identifier for filtering, a label for storing, sorting or faceting, plus a a set of synonyms 
for full text search and suggestions. Such a situation cannot be covered by the basic field descriptors, and to 
fill in that gap complex field descriptors where created.

A complex field descriptor is a field storing a simplified view of a java class, and which it is declared by providing 
the methods to calculate the values for each of the specific scopes desired.

```
SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
        .setFacet(true, tx -> Arrays.asList(tx.getId()))
        .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
        .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
        .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
        .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
        .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
        .setStored(true, tx -> tx.getTerm())
        .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);
```

The complex field definition has 3 types to be specified, the first one is the complex java class to be stored, in the previous 
example _Taxonomy_. The second one should be the returning type of the facet function, Integer in the example as the Id would be
the value used for the faceting. Finally a 3rd type for the sort scope. Suggestion and fulltext scope will be always expecting a 
String type return function.

_Note_:Facet, FullText and Suggest are design to be always multivalued so the functions providing their values should return an array of 
the expected type.   

Advance Filter
--------------

A new scope has been added to the complex filter, which only purpose is to do filtering. This field values should have the same type 
defined for faceting and it is always multivalued.

```
SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
        .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
        .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

```
  
Scoped filters
--------------

As with the complex fields it is possible to have different values for different scopes, the filters support the option to 
specify the scope in which they apply:
* Scope.Facet
* Scope.Filter
* Scope.Suggest

```
server.execute(Search.fulltext().filter(textComplexField.equals("uno",Scope.Filter)), assets);
```

_Note_: The default scope is facet.

Step 12: Contextualized fields
==============================

Searchlib supports the definition of document fields which can have different values for different contexts, allowing to get
 
```
final Document d1 = assets.createDoc("1")
        .setValue(numberField, 0f)
        .setContextualizedValue(numberField, "privateContext", 24f)
        .setContextualizedValue(numberField, "singleContext", 3f)
        .setValue(entityID, "123")
        .setValue(dateField, new Date());
```

The search context can be set when creating the search object by the method modifier _context("contextname")_:

```
final FulltextSearch searchAll = Search.fulltext().context("numberContext").filter(and(eq(entityID, "123"), eq(numberField,24f))).facet(entityID);
```

Those values which belong to a different context thant the one defined will not be in the search result.

```
final SearchResult searchResult = server.execute(searchAll, assets);
searchResult.getResults().get(0).getContextualizedValue(numberField, "numberContext"));
```


Step 13: Search modifiers
=========================

Strict search
-------------

By default every search is a _strict_ search which means that, having nested documents, no filters or search can 
be define for children using fields belonging just to the parent document factory.
 
By setting the search flag _strict_ to **false** nested document search will extend the defined filters or search queries in parent 
document fields to the children as if they had inherited the field itself. 

```
FulltextSearch search = Search.fulltext().setStrict(false).filter(eq(parent_value, "blue")).andChildrenSearch(child);
```

The previous example will return all the parent documents which field _parent_value_ has value "blue".

Geo distance
------------

The distance to the specified LatLong point will be calculated and added to the search result for e Every document with the field _locationgSingle_.

```
Search.fulltext().geoDistance(locationSingle,new LatLng(5,5))
```
   
Extension: Utils
================

**This is a work is in an alpha state**

In order to support developers we provide some Utilities.

MAM Metadata Provider
---------------------

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

