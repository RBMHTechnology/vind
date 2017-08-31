## 2. A simple Pojo search

Step one shows you, how quick and easy the search lib allows you to create a proper search on data items.

### 2.1. The dependency

The search lib is managed via maven/gradle repository. In our case we depend via maven dependency on the
embedded solr server. For production we will change this dependency to a remote solr server later.
```xml
<dependency>
    <groupId>com.rbmhtechnology.vind</groupId>
    <artifactId>embedded-solr-server</artifactId>
    <version>${vind.version}</version>
</dependency>
```

### 2.2. The pojo

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

### 2.3. Create and Index

We instantiate a search server just by getting an instance. As mentioned before, it is an instance
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

### 2.4 Search and delete

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
