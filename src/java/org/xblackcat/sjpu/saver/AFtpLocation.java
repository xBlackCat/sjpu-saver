package org.xblackcat.sjpu.saver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xblackcat.sjpu.utils.FilenameUtils;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 01.04.2015 19:39
 *
 * @author xBlackCat
 */
public class AFtpLocation implements ILocation {
    private static final Log log = LogFactory.getLog(FtpLocation.class);
    protected final String pass;
    protected final String userName;
    protected final String host;
    protected final int port;
    private final FTPClient ftp = new FTPClient();
    protected String path;

    public AFtpLocation() {
        host = target.getHost();
        port = target.getPort();
        path = target.getPath();
    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        validateConnect();

        String file = FilenameUtils.concat(this.path, path);

        SaverUtils.ensurePathExists(ftp, file.substring(0, file.lastIndexOf('/')));

        if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
            throw new IOException("Can't set binary mode");
        }
        OutputStream os = ftp.storeFileStream(file);

        if (os == null) {
            throw new IOException(
                    "Can not upload file " + file + " to server. Reply: " + ftp.getReplyString() + " (" + ftp.getReplyCode() + ")"
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
        if (!ftp.completePendingCommand()) {
            throw new IOException("Can't save data to ftp://" + host + (port == -1 ? "" : ":" + port) + file);
        }
    }

    @Override
    public void close() throws IOException {
        if (ftp.isConnected()) {
            try {
                ftp.logout();
            } finally {
                ftp.disconnect();
            }
        }
    }

    private void validateConnect() throws IOException {
        if (ftp.isConnected()) {
            return;
        }

        if (port == -1) {
            ftp.connect(host);
        } else {
            ftp.connect(host, port);
        }

        ftp.enterLocalPassiveMode();

        if (userName != null) {
            if (log.isTraceEnabled()) {
                log.trace("Make authorize as " + userName);
            }

            if (!ftp.login(userName, pass)) {
                throw new IOException("Can not log in to " + host + " as user " + userName);
            }
        }
    }
}
