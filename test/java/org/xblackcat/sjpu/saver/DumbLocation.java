package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.NotDirectoryException;

/**
 * 29.09.2016 11:47
 *
 * @author xBlackCat
 */
public class DumbLocation implements ILocation {
    public DumbLocation(URI unused) throws NotDirectoryException, IllegalArgumentException {
    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
