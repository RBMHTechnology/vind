## 3. Fulltext, Facets and Score

This step shows extended annotations and gives an overview on search.

### 3.1 Extended Annotations

We extend the Pojo from Step 1 with some more values. Depending on the role to play,
 the field is annotated accordingly.

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

### 3.2 Search Building

No let's try it out. You can see that the category field is used for fulltext search
and it influences the score more than the title. The other searches in the examples show
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

### 3.3 Paging

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

### 3.4 Slicing

The search results can be also requested in the format of slices by specifying an offset and an
slice size.

```java
//declaration of the search object and its slice
final FulltextSearch searchAll = Search.fulltext().slice(1, 10);

//get the results contained in the slice
final BeanSearchResult<NewsItem> result = server.execute(search, NewsItem.class);
```

### 3.5 Suggestions

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

*Note*: To query for suggestions on an specific field, it should previously have the suggest flag set
to true.