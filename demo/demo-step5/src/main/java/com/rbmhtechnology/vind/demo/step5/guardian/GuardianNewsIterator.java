package com.rbmhtechnology.vind.demo.step5.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

import java.io.IOException;
import java.util.List;
import java.net.URL;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class GuardianNewsIterator {

    private int page = 0;
    private String apiKey;

    public static int maxPageNumber = 10;
    public static int pagesize = 50;

    private static final String URL = "https://content.guardianapis.com/search?api-key=%s&page-size=%s&page=%s";

    private ObjectMapper mapper;

    public GuardianNewsResponse response;

    public GuardianNewsIterator(final String apiKey) {
        mapper = new ObjectMapper();
        mapper.registerModule(new JSR310Module());
        this.apiKey = apiKey;
    }

    public boolean hasNext() {
        return page < maxPageNumber && (response == null || response.getPages() > page);
    }

    public List<GuardianNewsItem> getNext() {
        try {
            URL url = new URL(String.format(URL,apiKey,pagesize, ++page));
            response = mapper.readValue(url, GuardianJsonResponse.class).getResponse();
            return response.getResults();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

}
