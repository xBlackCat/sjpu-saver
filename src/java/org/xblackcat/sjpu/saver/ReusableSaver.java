package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
        ILocation location = openSaver(target);

        location.save(target.getPath(), data, compression);
    }

    private ILocation openSaver(URI t) throws IOException {
        try {
            URI base = SaverUtils.getRootUri(t);

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
        final ILocation[] locations;
        lock.lock();
        try {
            locations = openSavers.values().stream().toArray(ILocation[]::new);
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
