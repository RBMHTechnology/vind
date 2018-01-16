package com.rbmhtechnology.vind.demo.step4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rbmhtechnology.vind.demo.step4.service.SearchService;
import com.rbmhtechnology.vind.api.result.SearchResult;

public class SearchApplication {

	public static void main(String[] args) throws JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		mapper.registerModule(new JavaTimeModule());

		try (SearchService searchService = new SearchService()) {

			//index
			searchService.index();

			//search
			SearchResult result = searchService.news("vind");

			System.out.println(mapper.writeValueAsString(result));
		}

	}

}
