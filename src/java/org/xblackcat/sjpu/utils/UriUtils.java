package org.xblackcat.sjpu.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 14.02.13 10:28
 *
 * @author xBlackCat
 */
public class UriUtils {
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

    /**
     * Create a new URI object which constructed from given by appending to path part a suffix
     *
     * @param uri    original URI object
     * @param suffix suffix to be appended to path part of original URI
     * @return a new URI object with modified path part.
     */
    public static URI appendToPath(URI uri, String suffix) {
        StringBuilder sb = new StringBuilder();
        final String scheme = uri.getScheme();
        if (scheme != null) {
            sb.append(scheme);
            sb.append(':');
        }
        if (uri.isOpaque()) {
            final String schemeSpecificPart = uri.getRawSchemeSpecificPart();
            sb.append(schemeSpecificPart);
        } else {
            final String host = uri.getHost();
            final String authority = uri.getRawAuthority();
            if (host != null) {
                sb.append("//");
                final String userInfo = uri.getRawUserInfo();
                if (userInfo != null) {
                    sb.append(userInfo);
                    sb.append('@');
                }
                boolean needBrackets = ((host.indexOf(':') >= 0)
                        && !host.startsWith("[")
                        && !host.endsWith("]"));
                if (needBrackets) sb.append('[');
                sb.append(host);
                if (needBrackets) sb.append(']');
                int port = uri.getPort();
                if (port != -1) {
                    sb.append(':');
                    sb.append(port);
                }
            } else if (authority != null) {
                sb.append("//");
                sb.append(authority);
            }
            final String path = uri.getPath();
            if (path != null) {
                sb.append(path);
            }
            sb.append(suffix);

            final String query = uri.getRawQuery();
            if (query != null) {
                sb.append('?');
                sb.append(query);
            }
        }
        final String fragment = uri.getRawFragment();
        if (fragment != null) {
            sb.append('#');
            sb.append(fragment);
        }

        return URI.create(sb.toString());
    }
}
