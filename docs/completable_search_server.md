## 8. Completable Search Server

In some cases a non-blocking search server is useful. the Completable Search Server uses Java CompletableFuture 
and is implemented as a wrapper arround the existing search server. It can be instantiated
 with an Executor or uses (by default) a FixedThreadPool with 16 threads. This number is configurable via
 SearchConfiguration parameter `application.executor.threads`.
 
```java
CompletableSearchServer server = new CompletableSearchServer(SearchServer.getInstance());

CompletableFuture<SearchResult> resultFuture = server.executeAsync(Search.fulltext(),factory);
```