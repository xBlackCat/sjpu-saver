package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * 19.06.2014 10:18
 *
 * @author xBlackCat
 */
class SaverUtils {
    private static final Log log = LogFactory.getLog(SaverUtils.class);

    final static Pattern PARAM_FETCHER = Pattern.compile("([-\\w]*)=(.*)");

    static String decode(String s) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, "UTF-8");
    }

    protected static void mkdirs(ChannelSftp channel, String path) throws SftpException {
        int i = path.lastIndexOf('/');
        if (i >= 0) {
            String parentPath = path.substring(0, i);
            boolean isDir = isDir(channel, parentPath);
            if (!isDir) {
                mkdirs(channel, parentPath);
            }
        }

        channel.mkdir(path);
    }

    protected static boolean isDir(ChannelSftp channel, String parentPath) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Check parent folder for existence: " + parentPath);
            }
            SftpATTRS stat = channel.stat(parentPath);
            return stat != null && stat.isDir();
        } catch (SftpException e) {
            // Not found
            if (log.isTraceEnabled()) {
                log.trace("Folder " + parentPath + " is not exists.");
            }
            return false;
        }

    }

    static void ensurePathExists(FTPClient ftp, String path) throws IOException {
        if (path == null) {
            throw new NullPointerException("Empty or null path");
        }

        if (path.length() == 0 || "/".equals(path)) {
            return;
        }

        if (ftp.changeWorkingDirectory(path)) {
            return;
        }

        ensurePathExists(ftp, path.substring(0, path.lastIndexOf('/')));

        if (!ftp.makeDirectory(path)) {
            throw new IOException("Can't create FTP folder " + path);
        }
    }
}
