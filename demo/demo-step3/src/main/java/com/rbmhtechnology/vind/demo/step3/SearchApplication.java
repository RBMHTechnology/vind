package com.rbmhtechnology.vind.demo.step3;

import com.rbmhtechnology.vind.demo.step3.service.SearchService;
import com.rbmhtechnology.vind.api.result.SearchResult;

public class SearchApplication {

	public static void main(String[] args) {

		try (SearchService searchService = new SearchService()) {

			//index
			searchService.index();

			//search
			SearchResult result = searchService.search("vind", 1);

			System.out.println(result);
		}

	}



}
