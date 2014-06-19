package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * 19.06.2014 11:22
 *
 * @author xBlackCat
 */
public class ReusableSaver implements ISaver, AutoCloseable {
    private final Map<URI, ILocation> openSavers = new HashMap<>();

    @Override
    public void save(URI target, InputStream data, Compression compression) throws IOException {
        String path = ("sftp".equalsIgnoreCase(target.getScheme())) ? StringUtils.substring(target.getPath(), 1) : target.getPath();

        ILocation saver = openSaver(target);

        saver.save(path, data, compression);
    }

    private ILocation openSaver(URI t) throws IOException {
        try {
            URI base = new URI(
                    t.getScheme(),
                    t.getUserInfo(),
                    StringUtils.defaultIfBlank(t.getHost(), ""),
                    t.getPort(),
                    "/",
                    null,
                    null
            );

            ILocation p = openSavers.get(base);
            if (p != null) {
                return p;
            }

            p = Saver.open(base);
            openSavers.put(base, p);

            return p;
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        for (ILocation saver : openSavers.values()) {
            try {
                saver.close();
            } catch (IOException e) {
                // ignore
            }
        }

        openSavers.clear();
    }
}
