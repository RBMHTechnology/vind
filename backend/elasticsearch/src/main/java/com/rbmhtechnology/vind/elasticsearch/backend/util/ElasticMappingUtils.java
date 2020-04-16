package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.elasticsearch.backend.ElasticSearchServer;
import com.rbmhtechnology.vind.elasticsearch.backend.MappingValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ElasticMappingUtils {

    private static final String MAPPINGS_JSON = "mappings.json";
    private static final String SETTINGS_JSON = "settings.json";
    private static final Logger log = LoggerFactory.getLogger(ElasticMappingUtils.class);

    private ElasticMappingUtils() {}

    public static String getDefaultMapping() {
        final URL resource = Optional.ofNullable(
                ElasticMappingUtils.class.getClassLoader().getResource(MAPPINGS_JSON))
                .orElseThrow(() -> new MappingValidationException(
                        String.format("Error getting default mapping file %s resource", MAPPINGS_JSON)));
        final Path mappingsFile = Paths.get(resource.getPath());

        try {
            return new String(Files.readAllBytes(mappingsFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SearchServerException(
                    String.format(
                            "Error reading default mapping definition from %s",
                            mappingsFile.toAbsolutePath()));
        }
    }

    public static String getDefaultSettings() {
        final URL resource = Optional.ofNullable(
                ElasticMappingUtils.class.getClassLoader().getResource(SETTINGS_JSON))
                .orElseThrow(() -> new MappingValidationException(
                        String.format("Error getting default settings file %s resource", SETTINGS_JSON)));
        final Path mappingsFile = Paths.get(resource.getPath());

        try {
            return new String(Files.readAllBytes(mappingsFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SearchServerException(
                    String.format(
                            "Error reading default settings definition from %s",
                            mappingsFile.toAbsolutePath()));
        }
    }

    public static void checkMappingsCompatibility(Map<String, Object> localMappings, Map<String, Object> remoteMappings, String remoteIndex) {
        final List<String> validationErrors = new ArrayList<>();

        final Map<String, Object> localFields = (Map<String, Object>)localMappings.get("properties");
        final Map<String, Object> localDynamicFields =
                ((List<Map<String, Object>>)localMappings.get("dynamic_templates")).stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .map(map -> (Map<String, Object>) map.getValue())
                .collect(Collectors.toMap(
                        m -> (String) m.get("match"),
                        m -> m.get("mapping")));

        final Map<String, Object> remoteFields = (Map<String, Object>) remoteMappings.get("properties");
        final Map<String, Object> remoteDynamicFields =
                ((List<Map<String, Object>>) remoteMappings.get("dynamic_templates")).stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .map(map -> (Map<String, Object>) map.getValue())
                .collect(Collectors.toMap(
                        m -> (String) m.get("match"),
                        m -> m.get("mapping")));

        if(localFields.size() == remoteFields.size()
                && localDynamicFields.size() == remoteDynamicFields.size()) {

            // check if all local fields are defined in remote server index mapping
            localFields.entrySet()
                    .stream()
                    .filter(field -> !remoteFields.containsKey(field.getKey()))
                    .forEach(field -> validationErrors.add(
                            String.format(
                                    "Remote index %s does not have field %s defined",
                                    remoteIndex,
                                    field.getKey())));

            localDynamicFields.entrySet()
                    .stream()
                    .filter(field -> !remoteDynamicFields.containsKey(field.getKey()))
                    .forEach(field -> validationErrors.add(
                            String.format(
                                    "Remote index %s does not have field %s defined",
                                    remoteIndex,
                                    field.getKey())));

            // check if all local fields are defined the same way as in the remote index
            localFields.entrySet()
                    .stream()
                    .filter(field -> remoteFields.containsKey(field.getKey()))
                    .filter(field -> !remoteFields.get(field.getKey()).equals(field.getValue()))
                    .forEach(field -> validationErrors.add(
                            String.format(
                                    "Remote index %s has a different field %s defined",
                                    remoteIndex,
                                    field.getKey())));

            localDynamicFields.entrySet()
                    .stream()
                    .filter(field -> remoteDynamicFields.containsKey(field.getKey()))
                    .filter(field -> !remoteDynamicFields.get(field.getKey()).equals(field.getValue()))
                    .forEach(field -> validationErrors.add(
                            String.format(
                                    "Remote index %s has a different field %s defined",
                                    remoteIndex,
                                    field.getKey())));
        }

        if(!validationErrors.isEmpty()) {
            final String errorMessage = String.format(
                    "Remote elasticsearch index mappings do not match current elasticsearch backend version: %s",
                    String.join(", ", validationErrors));

            log.error(errorMessage);
            throw new MappingValidationException(errorMessage);
        }
    }
}
