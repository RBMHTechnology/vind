package co.redlink.demo.step4;

import co.redlink.demo.step4.service.SearchService;
import com.rbmhtechnology.vind.api.result.SearchResult;

public class SearchApplication {

	public static void main(String[] args) {

		try (SearchService searchService = new SearchService()) {

			//index
			searchService.index();

			//search
			SearchResult result = searchService.news("redbull");

			System.out.println(result);
		}

	}

}
