package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 19.06.2014 11:22
 *
 * @author xBlackCat
 */
public class ReusableSaver implements ISaver {
    private final Map<URI, ILocation> openSavers = new HashMap<>();
    private final Lock lock = new ReentrantLock();

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

            lock.lock();
            try {
                ILocation p = openSavers.get(base);
                if (p != null) {
                    return p;
                }

                p = SaverUtils.openLocation(base);
                openSavers.put(base, p);
                return p;
            } finally {
                lock.unlock();
            }
        } catch (URISyntaxException e) {
            throw new MalformedURLException(e.getMessage());
        }
    }

    @Override
    public void close() {
        final Collection<ILocation> locations;
        lock.lock();
        try {
            locations = openSavers.values();
            openSavers.clear();
        } finally {
            lock.unlock();
        }

        for (ILocation saver : locations) {
            try {
                saver.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
