package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * 19.06.2014 10:18
 *
 * @author xBlackCat
 */
class SaverUtils {
    private static final Log log = LogFactory.getLog(SaverUtils.class);

    final static Pattern PARAM_FETCHER = Pattern.compile("([-\\w]*)(?:=(.*))?");

    static String decode(String s) throws UnsupportedEncodingException {
        if (s == null) {
            return null;
        }
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
        try (IUploadLocation saver = open(target)) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
                saver.upload(is, compression);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public static IUploadLocation open(URI basePath) throws IOException {
        return new Uploader(openLocation(basePath));
    }

    static ILocation openLocation(URI basePath) throws IOException {
        switch (basePath.getScheme()) {
            case "file":
                return new FileLocation(basePath);
            case "ftps":
                return new FtpsLocation(basePath);
            case "ftp":
                return new FtpLocation(basePath);
            case "sftp":
                return new SftpLocation(basePath);
            default:
                throw new MalformedURLException("unknown protocol: " + basePath.getScheme());
        }
    }
}
