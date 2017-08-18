package co.redlink.demo.step3;

import co.redlink.demo.step3.service.SearchService;
import com.rbmhtechnology.vind.api.result.SearchResult;

public class SearchApplication {

	public static void main(String[] args) {

		try (SearchService searchService = new SearchService()) {

			//index
			searchService.index();

			//search
			SearchResult result = searchService.search("redbull", 1);

			System.out.println(result);
		}

	}



}
