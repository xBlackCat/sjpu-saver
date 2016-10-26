package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * 27.02.13 15:45
 *
 * @author xBlackCat
 */
public interface ISaver extends AutoCloseable {
    void save(URI target, InputStream data, Compression compression) throws IOException;

    @Override
    default void close() {
    }
}
