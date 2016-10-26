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

/**
 * 19.06.2014 10:18
 *
 * @author xBlackCat
 */
class SaverUtils {
    private static final Log log = LogFactory.getLog(SaverUtils.class);

    private static final String SCHEMA_PACKAGE = "org.xblackcat.sjpu.saver.";
    private static final String CLASS_SUFFIX = "Location";

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
        try (ISaver saver = new Saver(SaverUtils::openLocation)) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
                saver.save(target, is, compression);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    static ILocation openLocation(URI basePath) throws IOException {
        final String proto = basePath.getScheme();
        final Constructor<?> locationConstructor;
        try {
            final String className = buildClassName(proto);
            final Class<?> aClass = Class.forName(className);
            if (!ILocation.class.isAssignableFrom(aClass)) {
                throw new IOException("Protocol saver implementation class should implements " + ILocation.class);
            }
            locationConstructor = aClass.getDeclaredConstructor(URI.class);
            if (log.isTraceEnabled()) {
                log.trace("Protocol " + proto + " is initialized");
            }
        } catch (ReflectiveOperationException | LinkageError e1) {
            log.trace("Class for protocol implementation can't be initialized. " + proto + " protocol will be disabled", e1);
            throw new MalformedURLException("Protocol is not supported: " + proto);
        }

        try {
            return (ILocation) locationConstructor.newInstance(basePath);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to initialize locator for scheme " + proto, e);
        }
    }

    private static String buildClassName(String proto) {
        final StringBuilder builder = new StringBuilder(SCHEMA_PACKAGE);
        builder.append(Character.toUpperCase(proto.charAt(0)));
        for (int i = 1; i < proto.length(); i++) {
            builder.append(Character.toLowerCase(proto.charAt(i)));
        }
        builder.append(CLASS_SUFFIX);
        return builder.toString();
    }
}
