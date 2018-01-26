package org.xblackcat.sjpu.saver;

import org.xblackcat.sjpu.util.function.FunctionEx;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

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
        try (ILocation file = locationProvider.apply(SaverUtils.getRootUri(target))) {
            file.save(target.getPath(), data, compression);
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }
}
