package com.rbmhtechnology.vind.demo.step1;

import com.rbmhtechnology.vind.demo.step1.model.NewsItem;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.report.ReportingSearchServer;
import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.writer.LogReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.ZonedDateTime;

public class SearchApplication {

	private static Logger logger = LoggerFactory.getLogger(SearchApplication.class);

	public static void main(String[] args) {

		//get an instance of a server (in this case a embedded solr server)
		try (SearchServer server = SearchServer.getInstance()) {

			final ReportWriter writer = new LogReportWriter();
			final ReportingSearchServer reportingSearchServer = new ReportingSearchServer(server);

			//index 2 news items
			NewsItem i1 = new NewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3));
			NewsItem i2 = new NewsItem("2", "Vind instance available", ZonedDateTime.now());

			server.indexBean(i1);
			server.indexBean(i2);
			logger.info("indexed beans.");

			//don't forget to commit
			server.commit();

			//a first (empty) search, which should retrieve all
			BeanSearchResult<NewsItem> result = reportingSearchServer.execute(Search.fulltext(), NewsItem.class);

			//e voila, 2 news items are returned
			assert result.getNumOfResults() == 2;

			//delete an item
            reportingSearchServer.deleteBean(i1);
            reportingSearchServer.commit();

			//search again
			result = reportingSearchServer.execute(Search.fulltext(), NewsItem.class);

			assert result.getNumOfResults() == 1;
			assert result.getResults().get(0).getId().equals("2");

		}

	}

}
