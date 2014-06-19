package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;

/**
 * 18.06.2014 16:53
 *
 * @author xBlackCat
 */
interface ILocation extends AutoCloseable {
    void save(String path, InputStream data, Compression compression) throws IOException;

    @Override
    void close() throws IOException;
}
