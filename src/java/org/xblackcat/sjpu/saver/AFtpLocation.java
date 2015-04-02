package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xblackcat.sjpu.utils.FilenameUtils;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.*;
import java.net.URI;

/**
 * 01.04.2015 19:39
 *
 * @author xBlackCat
 */
public abstract class AFtpLocation<F extends FTPClient> implements ILocation {
    protected static final Log log = LogFactory.getLog(FtpLocation.class);
    private final F ftpClient;
    protected final ParsedUri uri;

    public AFtpLocation(URI target) throws IOException {
        uri = ParsedUri.parse(target);

        this.ftpClient = buildClient(uri.getParams());
    }

    protected abstract F buildClient(ParsedUri.Param[] params);

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        validateConnect();

        String file = FilenameUtils.concat(uri.getPath(), path);

        SaverUtils.ensurePathExists(ftpClient, StringUtils.substringBeforeLast(file, "/"));

        if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
            throw new IOException("Can't set binary mode");
        }
        OutputStream os = ftpClient.storeFileStream(file);

        if (os == null) {
            throw new IOException(
                    "Can not upload file " + file + " to server. Reply: " + ftpClient.getReplyString() +
                            " (" + ftpClient.getReplyCode() + ")"
            );
        }

        if (log.isTraceEnabled()) {
            log.trace("Writing data...");
        }

        try {
            os = compression.cover(new BufferedOutputStream(os));

            IOUtils.copy(data, os);

            os.flush();
        } finally {
            os.close();
        }
        if (!ftpClient.completePendingCommand()) {
            throw new IOException("Can't save data to ftp://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort()) + file);
        }
    }

    @Override
    public void close() throws IOException {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
            } finally {
                ftpClient.disconnect();
            }
        }
    }

    protected void validateConnect() throws IOException {
        if (ftpClient.isConnected()) {
            return;
        }

        if (uri.getPort() == -1) {
            ftpClient.connect(uri.getHost());
        } else {
            ftpClient.connect(uri.getHost(), uri.getPort());
        }

        ftpClient.enterLocalPassiveMode();

        if (uri.getUser() != null) {
            if (log.isTraceEnabled()) {
                log.trace("Make authorize as " + uri.getUser());
            }

            if (!ftpClient.login(uri.getUser(), new String(uri.getPassword()))) {
                throw new IOException(
                        "Can not log in to " + uri.getHost() + " as user " + uri.getUser() + ". Reply " +
                                ftpClient.getReplyString() + " (" + ftpClient.getReplyCode() + ")"
                );
            }
        }
    }
}
