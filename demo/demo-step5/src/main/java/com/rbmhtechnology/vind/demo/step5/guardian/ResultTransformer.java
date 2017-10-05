package com.rbmhtechnology.vind.demo.step5.guardian;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import spark.ResponseTransformer;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 07.07.16.
 */
public class ResultTransformer implements ResponseTransformer {

    private ObjectMapper mapper = new ObjectMapper();

    public ResultTransformer() {
        mapper.registerModule(new JSR310Module());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String render(Object o) throws Exception {
        return mapper.writeValueAsString(o);
    }

}
