package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;

/**
 * 04.03.2015 15:34
 *
 * @author xBlackCat
 */
class Uploader implements IUploadLocation {
    private final ILocation location;

    public Uploader(ILocation location) {
        this.location = location;
    }

    @Override
    public void upload(InputStream data, Compression compression) throws IOException {
        location.save(null, data, compression);
    }

    @Override
    public void close() throws IOException {
        location.close();
    }
}
