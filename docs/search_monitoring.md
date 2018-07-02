## 9. Search Monitoring

Information about the activity of the users and how the tool performs is, for any information discovery system, of
mayor relevance in order to improve the behavior, provide more accurate results and track the possible errors. To
accomplish this easily, Vind provides a wrapper of the classic `SearchServer`, called `MonitoringSearchServer`,
which addresses this issue by recording full-text and suggestion searches.

Vind monitoring is divided in three main parts: API, writers and analysis. The *API* is meant to provide the basics
for custom implementations, the data model for the entries and the main `MonitoringSearchServer` class.

```xml
<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>monitoring-api</artifactId>
  <version>1.1.0</version>
</dependency>
```
While the *writers* module provide generic monitoring format writers, currently implementing a Log and an ElasticSearch writers.

```xml
<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>elastic-writer</artifactId>
  <version>1.1.0</version>
</dependency>

<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>log-writer</artifactId>
  <version>1.1.0</version>
</dependency>
```

Finally, the *anlysis* module is meant to provide pre-defined search analyzers aiming to extract relevant information
out of the monitoring entries. At the moment, just a basic reporting analyzer is implemented generating statistical
reports on search usage.

```xml
<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>monitoring-analysis</artifactId>
  <version>1.1.0</version>
</dependency>
```

### 9.1 The monitoring writers

Before having an instance of our `MonitoringServer`, a previous step is to choose a monitoring writer which behavior fits
the use case. Monitoring writers are meant to record the search executions in the desired format/storage(file, DB,...).
There are two default available writers included in **monitoring-writers** module: an _Elastic Search writer_ and a _Log writer_.
It is also possible to implement custom writers, which should extend the abstract class `com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter`,
provided in the **monitoring-api** artifact.

In this example we will use a simple testing `MonitoringWriter` which stores the entry logs in a list.

```java
public class SimpleMonitoringWriter extends MonitoringWriter {

    public final List<Log> logs = new ArrayList<>();

    @Override
    public void log(Log log) {
        logs.add(log);
    }
}
```  
**_Note:_** This writer obviously has memory issues, do no use it as it is in a real use case.

#### 9.1.1 Log writer

Based on the facade provided by Simple Logging Facade for Java ([SLF4J](http://www.slf4j.org)), this implementation of the `MonitoringWriter` records
the monitoring entries serialized as Json using the, provided by the end user, logging framework.

```xml
<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>log-writer</artifactId>
  <version>1.1.0</version>
</dependency>
```

#### 9.1.2 ElasticSearch writer

The monitoring entries are stored as Json in an ElasticSearch instance provided by the end user.

```xml
<dependency>
  <groupId>com.rbmhtechnology.vind</groupId>
  <artifactId>elastic-writer</artifactId>
  <version>1.1.0</version>
</dependency>
```

### 9.2 The Monitoring server

The `MonitoringSearchServer` is a wrapper over `SearchServer` and can be used with any **Vind** backend. It
allows to set sessions so the entries can be identified in a later analysis process.

```java
final Session session = new UserSession("213", "jdoe", "John Doe");
final MonitoringSearchServer monitoringServer = new MonitoringSearchServer(server, session);

//it can be used as a classic SearchServer
monitoringServer.execute(Search.fulltext(),factory);
```

To enrich the monitoring entries, it is also possible to instantiate the `MonitoringServer` providing more info about the
application which is performing the search and the current session.

```java
//describe your application
final Application myApp = new InterfaceApplication("myAppName", "version-0.1.0",new Interface("myInterface", "version-0.0.1"));

//instantiate a session object
final Session currentSession = new UserSession("sessionID", new User("userName", "userID", "user.contact@example.org"));

//Get an instance of your report writer
final ReportWriter writer = ReportWriter.getInstance();

//get an instance of the monitoring server
final SearchServer monitoringServer = new MonitoringSearchServer(SearchServer.getInstance(), myApp, currentSession, writer);
```

**_Note:_** If no configuration is provided for the application the ReportingSearchServer will try to load from the Vind
configuration the property _'search.monitoring.application.id'_. If it does not exist an exception will be thrown.

Additionally, it is possible to add custom information to the entries in two different ways: 
* by setting general metadata in the `MonitoringServer`, added to all activity record created by this server.
* by setting action specific metadata, just recorded for the current search execution.

```java
//get an instance of the monitoring server
final MonitoringSearchServer monitoringServer = new MonitoringSearchServer(SearchServer.getInstance(), myApp, currentSession, writer);

// Add a custom metadata entry which is applied to all the entries from this server.
monitoringServer.addMetadata("Module", "demo1-module");

final HashMap<String, Object> specificMetadata = new HashMap<>();
specificMetadata.put("Action", "demo2-specific-action")

//Add a custom metadata property for this specific search entry.
monitoringServer.execute(Search.fulltext(),factory, metadata);
```

### 9.3 The monitoring entry
A sample of a monitoring entry serialized as Json is displayed below:
```json
{
  "metadata": {
    "module": "demo-2"
  },
  "type": "fulltext",
  "application": {
    "name": "Application name",
    "version": "0.0.0",
    "id": "Application name - 0.0.0",
    "interface": {
      "name": "test-interface",
      "version": "0.0.0"
      }
  },
  "session": {
    "sessionId": "user 3",
    "user": {
      "name": "user 3",
      "id": "user-ID-3",
      "contact": null
    }
  },
  "timeStamp": "2018-03-15T13:14:14.141+01:00",
  "request": {
    "query": "*",
    "filter": {
      "type": "TermFilter",
      "field": "kind",
      "term": "blog",
      "scope": "Facet"
    },
    "facets": [
      {
        "type": "TermFacet",
        "scope": "Facet",
        "field": "kind"
      },
      {
        "type": "TermFacet",
        "scope": "Facet",
        "field": "category"
      }
    ],
    "rawQuery": "q=*&fl=*,score&qf=dynamic_single_none_kind^1.0+dynamic_single_en_title^1.0+dynamic_multi_en_category^1.2&defType=edismax&fq=_type_:NewsItem&fq=dynamic_single_facet_string_kind:\"blog\"&start=0&rows=10"
  },
  "response": {
    "num_of_results": 1,
    "query_time": 2,
    "elapsed_time": 3,
    "vind_time": 5
  },
  "sorting": [
    {
      "type": "SimpleSort",
      "field": "created",
      "direction": "Desc"
    }
  ],
  "paging": {
    "index": 1,
    "size": 10,
    "type": "page"
  }
}
```

### 9.4 Reporting
Vind gives you a simple to use API for creating reports based on monitoring. The reports include:

* Usage overal
* Usage of searchs strings
* Usage of scoped filters (e.g. facets etc.)

The reports can be created on certain timerange:

```java
 //configure at least appId and connection (in this case elastic search)

final ElasticSearchReportConfiguration config = new ElasticSearchReportConfiguration()
        .setApplicationId("myApp")
        .setConnectionConfiguration(new ElasticSearchConnectionConfiguration(
                "1.1.1.1",
                "1920",
                "logstash-2018.*"
        ));

//create service with config and timerange

ZonedDateTime to = ZonedDateTime.now();
ZonedDateTime from = to.minus(1, ChronoUnit.WEEKS);

ReportService service = new ElasticSearchReportService(config, from, to);

//create report and serialize as HTML

Report report = service.generateReport();

new HtmlReportWriter().write(report, "/tmp/myreport.html");

```

TODO: extend this description in a later release