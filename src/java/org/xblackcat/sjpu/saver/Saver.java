package org.xblackcat.sjpu.saver;

import org.xblackcat.sjpu.util.function.FunctionEx;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Wrapper for {@linkplain ILocation} implementation
 *
 * @author xBlackCat
 */
public class Saver implements ISaver {
    private final FunctionEx<URI, ILocation, IOException> locationProvider;

    public Saver() {
        this(SaverUtils::openLocation);
    }

    public Saver(FunctionEx<URI, ILocation, IOException> locationProvider) {
        this.locationProvider = locationProvider;
    }

    @Override
    public void save(final URI target, InputStream data, Compression compression) throws IOException {
        try (ILocation file = locationProvider.apply(target)) {
            file.save("", data, compression);
        }
    }
}
