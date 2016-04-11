package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.NotDirectoryException;

/**
 * 18.06.2014 17:18
 *
 * @author xBlackCat
 */
public class FileLocation implements ILocation {
    private static final Log log = LogFactory.getLog(FileLocation.class);

    private File base;

    public FileLocation(URI uri) throws NotDirectoryException, IllegalArgumentException {
        this.base = new File(uri);

        if (base.exists() && !base.isDirectory()) {
            throw new NotDirectoryException("Existing base file is not a directory: " + base.getAbsolutePath());
        }
    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        File destination;
        if (StringUtils.startsWith(path, "/")) {
            destination = new File(path);
        } else {
            destination = new File(base, path);
        }
        if (log.isTraceEnabled()) {
            log.trace("Saving to file " + destination.getAbsolutePath());
        }
        File parentFile = destination.getParentFile();
        if (!parentFile.isDirectory()) {
            log.trace("Create parent folder(s): " + parentFile.getAbsolutePath());

            if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
                throw new IOException("Can not create destination folder: " + parentFile.getAbsolutePath());
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Writing data...");
        }
        try (OutputStream os = compression.cover(new BufferedOutputStream(new FileOutputStream(destination)))) {
            IOUtils.copy(data, os);
            os.flush();
        }
    }

    @Override
    public void close() throws IOException {
    }
}
