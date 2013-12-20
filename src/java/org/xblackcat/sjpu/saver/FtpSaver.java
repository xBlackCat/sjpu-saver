package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xblackcat.sjpu.utils.IOUtils;
import org.xblackcat.sjpu.utils.UriUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;

/**
 * 27.02.13 16:22
 *
 * @author xBlackCat
 */
public class FtpSaver implements ISaver {
    private static final Log log = LogFactory.getLog(FtpSaver.class);

    @Override
    public void save(URI target, InputStream data, Compression compression) throws IOException {
        FTPClient ftp = new FTPClient();
        try {
            if (target.getPort() == -1) {
                ftp.connect(target.getHost());
            } else {
                ftp.connect(target.getHost(), target.getPort());
            }

            ftp.enterLocalPassiveMode();

            if (target.getRawUserInfo() != null) {
                String[] userInfo = target.getRawUserInfo().split(":", 2);

                String pass = StringUtils.isEmpty(userInfo[1]) ? "" : URLDecoder.decode(userInfo[1], "UTF-8");
                String userName = URLDecoder.decode(userInfo[0], "UTF-8");

                if (log.isTraceEnabled()) {
                    log.trace("Make authorize as " + userName);
                }

                if (!ftp.login(userName, pass)) {
                    throw new IOException("Can not log in to " + target.getHost() + " as user " + userName);
                }
            }

            String file = target.getPath();

            ensurePathExists(ftp, file.substring(0, file.lastIndexOf('/')));

            if (!ftp.setFileType(FTP.BINARY_FILE_TYPE)) {
                throw new IOException("Can't set binary mode");
            }
            OutputStream os = ftp.storeFileStream(file);

            if (os == null) {
                throw new IOException(
                        "Can not upload file " + file + " to server. Reply: " +
                                ftp.getReplyString() +
                                " (" +
                                ftp.getReplyCode() +
                                ")"
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
                throw new IOException("Can't save data to " + UriUtils.toString(target));
            }
            ftp.logout();
        } finally {
            ftp.disconnect();
        }
    }

    private void ensurePathExists(FTPClient ftp, String path) throws IOException {
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
