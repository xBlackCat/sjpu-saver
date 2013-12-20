package org.xblackcat.sjpu.saver;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.xblackcat.sjpu.utils.IOUtils;

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
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            OutputStream os = ftp.storeFileStream(file);

            if (os == null) {
                if (ftp.getReplyCode() != 550) {
                    throw new IOException(
                            "Can not upload file to server. Reply: " +
                                    ftp.getReplyString() +
                                    " (" +
                                    ftp.getReplyCode() +
                                    ")"
                    );
                }
                // Try to create target folder

                String folder = file.substring(0, file.lastIndexOf('/'));

                if (log.isTraceEnabled()) {
                    log.trace("Make folder: " + folder);
                }

                if (StringUtils.isNotEmpty(folder)) {
                    ftp.setFileType(FTP.ASCII_FILE_TYPE);
                    if (!ftp.makeDirectory(folder)) {
                        throw new IOException("Can not create target folder");
                    }
                }

                if (log.isTraceEnabled()) {
                    log.trace("Try to re-open file stream for file " + file);
                }
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                os = ftp.storeFileStream(file);
            }

            if (log.isTraceEnabled()) {
                log.trace("Writing data...");
            }

            try {
                os = compression.cover(new BufferedOutputStream(os));

                IOUtils.copy(data, os);
            } finally {
                os.close();
            }
        } finally {
            ftp.disconnect();
        }
    }
}
