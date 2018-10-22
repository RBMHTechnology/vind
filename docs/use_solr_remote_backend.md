## 7. Use Solr Remote Backend

In this step we show how to use a Remote Solr Server as backend. Additionally we learn how
change basic configurations of the search lib.

### 7.1 Dependencies

In order to use a Solr Remote Backend we have to switch the dependency to this.

```xml
<dependency>
    <groupId>com.rbmhtechnology.vind</groupId>
    <artifactId>remote-solr-server</artifactId>
    <version>${vind.version}</version>
</dependency>
```

### 7.2 Configuration

Vind supports various types for configuration, which configuration by *environment variables*, by *property file* or/and by *code interface*.
It comes with some basic configurations (like pagesize) that can be changed. The properties are overwritten following the ordering:
Default Properties < Environment Variables < Property File.

Currently the following properties are supported:

| Key | Type | Description |
| --- | --- | --- |
| *server.collection* | STRING | The solr collection name |
| *server.host* | STRING | The solr host or hostlist |
| *server.provider* | STRING | Fully qualified name of the solr provider |
| *server.connection.timeout* | LONG | Connection timeout for remote server |
| *server.so.timeout* | LONG | Zookeeper client timeout |
| *server.solr.cloud* | BOOL | If remote solr runs in cloud mode |
| *application.executor.threads* | INT | Max. parallel threads for async connection |
| *search.result.pagesize* | INT | Result pagesize |
| *search.result.showScore* | BOOL | Include score in the result objects |
| *search.result.facet.includeEmpty* | BOOL | Include empty facets |
| *search.result.facet.length* | INT | Length for facet list |
| *vind.properties.file* | STRING | Path to property file |

**Environment Properties**

Environment Properties have a slightly different format. They start with the prefix `VIND_`, are uppercased and the dots are replaced underscored.
So e.g. `server.solr.cloud` turns to `VIND_SERVER_SOLR_CLOUD`.

**Property File**

For configuring e.g the remote host you have to place a file called searchlib.properties in the classpath which includes the host information.

```
//configure http client
server.host=http://localhost:8983/solr/searchindex

//configure zookeeper client
server.host=zkServerA:2181,zkServerB:2181,zkServerC:2181
server.solr.cloud=true
server.solr.collection=collection1

//change pagesize
search.result.pagesize=7
```
 
**Code Interface**
 
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

** Vind Solr backend with Docker

We created a Docker image *redlinkgmbh/vind-solr-server* which even simplifies the setup of a Solr backend for Vind. The image is hosted on 
[Dockerhub](https://hub.docker.com/r/redlinkgmbh/vind-solr-server/tags/). There you can find different versions, whereby
the version number is aligned to the Vind release version. You can easily start a Vind Solr backend like this:
```
docker run -p 8983:8983 redlinkgmbh/vind-solr-server:1.3.0
```
This will start the server including the vind core. The configuration for the host is:
```properties
server.host=http://localhost:8983/solr/vind
```
