package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;

/**
 * 04.03.2015 15:34
 *
 * @author xBlackCat
 */
public interface IUploadLocation extends AutoCloseable {
    void upload(InputStream data, Compression compression) throws IOException;

    @Override
    void close() throws IOException;
}
