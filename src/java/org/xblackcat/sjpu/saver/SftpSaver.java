package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * 27.02.13 17:52
 *
 * @author xBlackCat
 */
public class SftpSaver implements ISaver {
    @Override
    public void save(final URI target, InputStream data, Compression compression) throws IOException {
        try (ILocation file = new SftpLocation(target)) {
            file.save("", data, compression);
        }
    }

}
