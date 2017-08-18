package com.rbmhtechnology.vind.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class FileSystemUtils {
        private static final Set<URI> jarFileSystems = new HashSet<>();

    /**
     * Convert a local URL (file:// or jar:// protocol) to a {@link Path}
     * @param resource the URL resource
     * @return the Path
     * @throws URISyntaxException
     * @throws IOException
     */
    public static Path toPath(URL resource) throws IOException, URISyntaxException {
        if (resource == null) return null;

        final String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
            return Paths.get(resource.toURI());
        } else if ("jar".equals(protocol)) {
            final String s = resource.toString();
            final int separator = s.indexOf("!/");
            final String entryName = s.substring(separator + 2);
            final URI fileURI = URI.create(s.substring(0, separator));

            final FileSystem fileSystem;
            synchronized (jarFileSystems) {
                if (jarFileSystems.add(fileURI)) {
                    fileSystem = FileSystems.newFileSystem(fileURI, Collections.<String, Object>emptyMap());
                } else {
                    fileSystem = FileSystems.getFileSystem(fileURI);
                }
            }
            return fileSystem.getPath(entryName);
        } else {
            throw new IOException("Can't read " + resource + ", unknown protocol '" + protocol + "'");
        }
    }

    private FileSystemUtils() {
        throw new AssertionError("No com.rbmhtechnology.vind.utils.FileSystemUtils instances for you!");
    }
}
