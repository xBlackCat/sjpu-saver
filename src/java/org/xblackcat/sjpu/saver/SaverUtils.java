package org.xblackcat.sjpu.saver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 19.06.2014 10:18
 *
 * @author xBlackCat
 */
class SaverUtils {
    private static final Log log = LogFactory.getLog(SaverUtils.class);

    private static final Map<String, Constructor<ILocation>> LOCATIONS;

    static {
        final Map<String, Constructor<ILocation>> map = new HashMap<>();
        addLocator(map, "file", "org.xblackcat.sjpu.saver.FileLocation");
        addLocator(map, "ftps", "org.xblackcat.sjpu.saver.FtpsLocation");
        addLocator(map, "ftp", "org.xblackcat.sjpu.saver.FtpLocation");
        addLocator(map, "sftp", "org.xblackcat.sjpu.saver.SftpLocation");

        LOCATIONS = Collections.unmodifiableMap(map);
    }

    private static void addLocator(Map<String, Constructor<ILocation>> map, String proto, String className) {
        Constructor<ILocation> result;
        try {
            @SuppressWarnings("unchecked")
            final Class<ILocation> aClass = (Class<ILocation>) Class.forName(className);
            result = aClass.getDeclaredConstructor(URI.class);
            map.put(proto, result);
            log.trace("Protocol " + proto + " is initialized");
        } catch (ReflectiveOperationException | LinkageError e) {
            log.trace("Class " + className + " can't be initialized. " + proto + " protocol will be disabled", e);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.err.println("Usage: java -jar sjpu-saver.jar <upload url> <filename> [<compression: one of Plain, GZip, XZ or BZip2>]");
            System.exit(1);
            return;
        }

        final String targetUri = args[0];
        final String fileName = args[1];
        final Compression compression;
        if (args.length > 2) {
            compression = Compression.valueOf(args[2]);
        } else {
            compression = Compression.Plain;
        }

        System.out.println("Upload file '" + fileName + "' to location " + targetUri);

        URI target = URI.create(targetUri);
        try (IUploadLocation saver = new Uploader(openLocation(target))) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
                saver.upload(is, compression);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    static ILocation openLocation(URI basePath) throws IOException {
        final Constructor<ILocation> locationConstructor = LOCATIONS.get(basePath.getScheme());
        if (locationConstructor == null) {
            throw new MalformedURLException("unknown protocol: " + basePath.getScheme());
        }

        try {
            return locationConstructor.newInstance(basePath);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to initialize locator for scheme " + basePath.getScheme(), e);
        }
    }
}
