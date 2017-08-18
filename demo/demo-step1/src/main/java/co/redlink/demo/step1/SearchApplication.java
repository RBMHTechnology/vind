package co.redlink.demo.step1;

import co.redlink.demo.step1.model.NewsItem;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;

import java.time.ZonedDateTime;

public class SearchApplication {

	public static void main(String[] args) {

		//get an instance of a server (in this case a embedded solr server)
		try (SearchServer server = SearchServer.getInstance()) {

			//index 2 news items
			NewsItem i1 = new NewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3));
			NewsItem i2 = new NewsItem("2", "Vind instance available", ZonedDateTime.now());

			server.indexBean(i1);
			server.indexBean(i2);

			//don't forget to commit
			server.commit();

			//a first (empty) search, which should retrieve all
			BeanSearchResult<NewsItem> result = server.execute(Search.fulltext(), NewsItem.class);

			//e voila, 2 news items are returned
			assert result.getNumOfResults() == 2;

			//delete an item
			server.deleteBean(i1);
			server.commit();

			//search again
			result = server.execute(Search.fulltext(), NewsItem.class);

			assert result.getNumOfResults() == 1;
			assert result.getResults().get(0).getId().equals("2");

		}

	}

}
