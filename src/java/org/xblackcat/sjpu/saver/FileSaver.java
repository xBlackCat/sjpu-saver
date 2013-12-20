package org.xblackcat.sjpu.saver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.*;
import java.net.URI;

/**
 * 27.02.13 16:44
 *
 * @author xBlackCat
 */
public class FileSaver implements ISaver {
    private static final Log log = LogFactory.getLog(FileSaver.class);

    @Override
    public void save(URI target, InputStream data, Compression compression) throws IOException {
        File destination = new File(target);
        if (log.isTraceEnabled()) {
            log.trace("Saving to file " + destination.getAbsolutePath());
        }
        File parentFile = destination.getParentFile();
        if (!parentFile.isDirectory()) {
            log.trace("Create parent folder: " + parentFile.getAbsolutePath());

            if (!parentFile.mkdirs()) {
                throw new IOException("Can not create destination folder: " + parentFile.getAbsolutePath());
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Writing data...");
        }
        try (OutputStream os = compression.cover(new BufferedOutputStream(new FileOutputStream(destination)))) {
            IOUtils.copy(data, os);
        }
    }
}
