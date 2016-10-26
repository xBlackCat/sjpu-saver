package org.xblackcat.sjpu.saver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 18.06.2014 17:18
 *
 * @author xBlackCat
 */
class FileLocation implements ILocation {
    private static final Log log = LogFactory.getLog(FileLocation.class);

    private Path base;

    public FileLocation(URI uri) throws NotDirectoryException, IllegalArgumentException {
        this.base = Paths.get(uri);

        if (Files.exists(base) && !Files.isDirectory(base)) {
            throw new NotDirectoryException("Existing base file is not a directory: " + base);
        }
    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        Path destination = base.resolve(path);

        if (log.isTraceEnabled()) {
            log.trace("Saving to file " + destination.toAbsolutePath());
        }
        Path parentFile = destination.getParent();
        if (!Files.isDirectory(parentFile)) {
            log.trace("Create parent folder(s): " + parentFile);

            Files.createDirectories(parentFile);
            if (!Files.isDirectory(parentFile)) {
                throw new IOException("Can not create destination folder: " + parentFile.toAbsolutePath());
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Writing data...");
        }
        try (OutputStream os = compression.cover(new BufferedOutputStream(Files.newOutputStream(destination)))) {
            IOUtils.copy(data, os);
            os.flush();
        }
    }

    @Override
    public void close() throws IOException {
    }
}
