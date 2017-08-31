## 9. Reporting

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