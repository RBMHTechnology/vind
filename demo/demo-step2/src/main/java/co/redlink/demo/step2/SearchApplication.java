package co.redlink.demo.step2;

import co.redlink.demo.step2.model.NewsItem;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.BeanPageResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;

import java.time.ZonedDateTime;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;

public class SearchApplication {

	public static void main(String[] args) {

		//get an instance of a server (in this case a embedded solr server)
		SearchServer server = SearchServer.getInstance();

		//index 2 news items
		NewsItem i1 = new NewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3), "article", "coding");
		NewsItem i2 = new NewsItem("2", "Vind instance available", ZonedDateTime.now(), "blog", "coding", "release");

		server.indexBean(i1);
		server.indexBean(i2);
		server.commit();

		//this search should retrieve news items that should match the search term best
		FulltextSearch search = Search.fulltext("redbull release");

		BeanSearchResult<NewsItem> result = server.execute(search, NewsItem.class);

		//lets log the results
		System.out.println("--- Search 1: Fulltext ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//now we want to have also the facets for category and kind.
		//additionally we change the query
		search.text("redbull");
		search.facet("category","kind");

		result = server.execute(search, NewsItem.class);

		System.out.println("--- Search 2.1: Category Facets ---");
		result.getFacetResults().getTermFacet("category",String.class).getValues().forEach(System.out::println);
		System.out.println();

		System.out.println("--- Search 2.2: Kind Facets ---");
		result.getFacetResults().getTermFacet("kind",String.class).getValues().forEach(System.out::println);
		System.out.println();

		//new we define a search order based on the 'created ' field
		search.sort(desc("created"));
		result = server.execute(search, NewsItem.class);

		System.out.println("--- Search 3: Sort by created descending ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//now we want to filter for all items with the kind 'blog'.
		result = server.execute(Search.fulltext().filter(eq("kind","blog")), NewsItem.class);

		System.out.println("--- Search 4: Filtered by kind=blog ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//this search should retrieve news items
		//we set the page to 1 and the pagesize to 1
		result = server.execute(Search.fulltext().page(1, 1), NewsItem.class);

		//lets log the results
		System.out.println("--- Search 5.1: Paging (Page 1) ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//the result itself supports paging, so we can loop the pages
		while(((BeanPageResult)result).hasNextPage()) {
			result = ((BeanPageResult)result).nextPage();
			System.out.println("--- Search 5.2: Paging (Page " + ((BeanPageResult)result).getPage() + ") ---");
			result.getResults().forEach(System.out::println);
			System.out.println();
		}

		//suggest
		SuggestionResult suggestions = server.execute(Search.suggest("c").fields("category"), NewsItem.class);
		System.out.println("--- Suggestions: ---");
		System.out.println(suggestions.get("category").getValues());
		System.out.println();

		//close the server
		server.close();
	}



}
