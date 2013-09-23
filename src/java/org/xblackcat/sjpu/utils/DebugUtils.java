package org.xblackcat.sjpu.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 14.02.13 10:28
 *
 * @author xBlackCat
 */
public class DebugUtils {
    public final static DateFormat DEBUG_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ROOT);

    public static String toString(URI uri) {
        try {
            return new URI(
                    uri.getScheme(),
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            ).toString();
        } catch (URISyntaxException e) {
            return uri.toString();
        }
    }
}
