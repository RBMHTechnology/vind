package com.rbmhtechnology.vind.demo.step2;

import com.rbmhtechnology.vind.demo.step2.model.NewsItem;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.BeanPageResult;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.monitoring.elastic.writer.ElasticWriter;
import com.rbmhtechnology.vind.monitoring.MonitoringSearchServer;
import com.rbmhtechnology.vind.monitoring.log.writer.LogWriter;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;
import com.rbmhtechnology.vind.monitoring.model.Interface.Interface;
import com.rbmhtechnology.vind.monitoring.model.application.InterfaceApplication;
import com.rbmhtechnology.vind.monitoring.model.session.UserSession;
import com.rbmhtechnology.vind.monitoring.model.user.User;

import java.time.ZonedDateTime;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;

public class SearchApplication {

	public static void main(String[] args) {

		//get an instance of a server (in this case a embedded solr server)
		SearchServer server = SearchServer.getInstance();


		final MonitoringWriter writer = new LogWriter();
		final InterfaceApplication application = new InterfaceApplication("Application name", "0.0.0", new Interface("sugar-love","0.0.0"));
		final MonitoringSearchServer monitoringSearchServer = new MonitoringSearchServer(server, application, writer);

		monitoringSearchServer.setSession( new UserSession("session-ID-1234567",new User("user 2","user-ID-2")));

		//index 2 news items
		NewsItem i1 = new NewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3), "article", "coding");
		NewsItem i2 = new NewsItem("2", "Vind instance available", ZonedDateTime.now(), "blog", "coding", "release");

		server.indexBean(i1);
		server.indexBean(i2);
		server.commit();

		//this search should retrieve news items that should match the search term best
		FulltextSearch search = Search.fulltext("vind release");

		BeanSearchResult<NewsItem> result = monitoringSearchServer.execute(search, NewsItem.class);

		//lets log the results
		System.out.println("\n--- Search 1: Fulltext ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//now we want to have also the facets for category and kind.
		//additionally we change the query
		search.text("vind");
		search.facet("category","kind");

		result = monitoringSearchServer.execute(search, NewsItem.class);

		System.out.println("\n--- Search 2.1: Category Facets ---");
		result.getFacetResults().getTermFacet("category",String.class).getValues().forEach(System.out::println);
		System.out.println();

		System.out.println("--- Search 2.2: Kind Facets ---");
		result.getFacetResults().getTermFacet("kind",String.class).getValues().forEach(System.out::println);
		System.out.println();

		monitoringSearchServer.setSession( new UserSession("session-ID-12345678",new User("user 3","user-ID-3")));

		//new we define a search order based on the 'created ' field
		search.sort(desc("created"));
		result = monitoringSearchServer.execute(search, NewsItem.class);

		System.out.println("\n--- Search 3: Sort by created descending ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//now we want to filter for all items with the kind 'blog'.
		result = monitoringSearchServer.execute(Search.fulltext().filter(eq("kind","blog")), NewsItem.class);

		System.out.println("\n--- Search 4: Filtered by kind=blog ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//this search should retrieve news items
		//we set the page to 1 and the pagesize to 1
		result = monitoringSearchServer.execute(Search.fulltext().page(1, 1), NewsItem.class);

		//lets log the results
		System.out.println("\n--- Search 5.1: Paging (Page 1) ---");
		result.getResults().forEach(System.out::println);
		System.out.println();

		//the result itself supports paging, so we can loop the pages
		while(((BeanPageResult)result).hasNextPage()) {
			result = ((BeanPageResult)result).nextPage();
			System.out.println("\n--- Search 5.2: Paging (Page " + ((BeanPageResult)result).getPage() + ") ---");
			result.getResults().forEach(System.out::println);
			System.out.println();
		}

		//suggest
		SuggestionResult suggestions = monitoringSearchServer.execute(Search.suggest("c").fields("category"), NewsItem.class);
		System.out.println("\n--- Suggestions: ---");
		System.out.println(suggestions.get("category").getValues());
		System.out.println();

		//close the server
		server.close();
	}



}
