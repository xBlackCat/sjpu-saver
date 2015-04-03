package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 02.04.2015 12:10
 *
 * @author xBlackCat
 */
public class ParsedUri {
    private static final Log log = LogFactory.getLog(ParsedUri.class);

    private final String host;
    private final int port;
    private final String path;
    private final String user;
    private final char[] password;
    private final Param[] params;

    public static ParsedUri parse(URI uri) throws UnsupportedEncodingException {
        final String host = uri.getHost();
        final int port = uri.getPort();
        final String path = uri.getPath();
        final String userName;
        final char[] pass;
        final List<Param> params = new ArrayList<>();

        if (uri.getRawUserInfo().indexOf(';') < 0) {
            String[] userInfo = StringUtils.split(uri.getRawUserInfo(), ':');

            userName = SaverUtils.decode(userInfo[0]);
            pass = (userInfo.length == 1 || StringUtils.isEmpty(userInfo[1])) ? null : SaverUtils.decode(userInfo[1]).toCharArray();
        } else {
            String[] info = StringUtils.split(uri.getRawUserInfo(), ';');

            final String[] paramStrs = StringUtils.split(info[1], ',');

            if (paramStrs.length > 0) {
                String lastItem = paramStrs[paramStrs.length - 1];
                int lastIndexOf = lastItem.lastIndexOf(':');
                if (lastIndexOf >= 0) {
                    // Possibly password is present
                    paramStrs[paramStrs.length - 1] = lastItem.substring(0, lastIndexOf);
                    pass = SaverUtils.decode(lastItem.substring(lastIndexOf + 1)).toCharArray();
                } else {
                    pass = null;
                }

                for (String param : paramStrs) {
                    final Matcher m = SaverUtils.PARAM_FETCHER.matcher(param);
                    if (!m.matches()) {
                        if (log.isWarnEnabled()) {
                            log.warn("Invalid parameter definition '" + param + "' in uri.");
                        }
                        continue;
                    }

                    params.add(new Param(m.group(1), SaverUtils.decode(m.group(2))));
                }
            } else {
                pass = null;
            }

            userName = SaverUtils.decode(info[0]);
        }

        return new ParsedUri(host, port, path, userName, pass, params.toArray(new Param[params.size()]));
    }

    public ParsedUri(String host, int port, String path, String user, char[] password, Param... params) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.user = user;
        this.password = password;
        this.params = params;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getUser() {
        return user;
    }

    public char[] getPassword() {
        return password;
    }

    public Param[] getParams() {
        return params;
    }

    public static final class Param {
        private final String name;
        private final String value;

        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
