## 14. Search modifiers

In this step the existing search modifiers are be described.

### 14.1 Strict search

By default every search is a _strict_ search which means that, having nested documents, no filters or search can 
be define for children using fields belonging just to the parent document factory.
 
By setting the search flag _strict_ to **false** nested document search will extend the defined filters or search queries in parent 
document fields to the children as if they had inherited the field itself. 

```java
FulltextSearch search = Search.fulltext().setStrict(false).filter(eq(parent_value, "blue")).andChildrenSearch(child);
```

The previous example will return all the parent documents which field _parent_value_ has value "blue".

### 14.2 Geo distance

The distance to the specified LatLong point will be calculated and added to the search result for e Every document with the field _locationgSingle_.

```java
Search.fulltext().geoDistance(locationSingle,new LatLng(5,5))
```
