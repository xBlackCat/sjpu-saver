package org.xblackcat.sjpu.saver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 27.02.13 15:45
 *
 * @author xBlackCat
 */
public class Saver implements ISaver {
    private final Map<String, String> savers;

    public Saver() {
        savers = new HashMap<String, String>();

        savers.put("ftp", "org.xblackcat.sjpu.saver.FtpSaver");
        savers.put("file", "org.xblackcat.sjpu.saver.FileSaver");
        savers.put("sftp", "org.xblackcat.sjpu.saver.SftpSaver");
    }

    private ISaver get(URI target) throws IllegalArgumentException {
        final String saver = savers.get(target.getScheme());
        if (saver != null) {
            try {
                return (ISaver) Class.forName(saver).newInstance();
            } catch (Throwable e) {
                throw new IllegalArgumentException("Saver is not installed for scheme " + target.getScheme(), e);
            }
        }

        throw new IllegalArgumentException("Saver is not installed for scheme " + target.getScheme());
    }

    @java.lang.Override
    public void save(
            URI target,
            InputStream data,
            Compression compression
    ) throws IOException, IllegalArgumentException {
        ISaver realSaver = get(target);

        realSaver.save(target, data, compression);
    }
}
