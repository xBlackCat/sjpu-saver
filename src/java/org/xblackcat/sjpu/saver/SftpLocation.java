package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.FilenameUtils;
import org.xblackcat.sjpu.utils.IOUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

/**
 * 18.06.2014 18:20
 *
 * @author xBlackCat
 */
class SftpLocation implements ILocation {
    private static final Log log = LogFactory.getLog(SftpSaver.class);

    private final ParsedUri uri;
    private final UserInfo userInfo;
    private byte[] passphrase;
    private HostKeyRepository fingerPrintAcceptor;
    private String privateKeyFile;

    private Session session;

    public SftpLocation(URI basePath) throws IOException {
        uri = ParsedUri.parse(basePath);

        privateKeyFile = null;
        String privateKeyPass = null;
        byte[] fingerPrint = null;
        boolean acceptAll = false;

        for (ParsedUri.Param param : uri.getParams()) {
            String name = param.getName();
            String value = param.getValue();

            switch (name.toLowerCase()) {
                case "accept-all":
                    acceptAll = true;
                    break;
                case "fingerprint":
                    String hexString = StringUtils.remove(value, '-');
                    fingerPrint = new byte[16];
                    ByteBuffer bb = ByteBuffer.wrap(fingerPrint);
                    bb.putInt((int) Long.parseLong(hexString.substring(0, 8), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(8, 16), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(16, 24), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(24, 32), 16));
                    break;
                case "private-key-file":
                    privateKeyFile = value;
                    break;
                case "pk-passphrase":
                    privateKeyPass = value;
                    break;
            }
        }

        if (privateKeyFile != null) {
            passphrase = privateKeyPass != null ? privateKeyPass.getBytes(StandardCharsets.UTF_8) : null;
        }

        if (acceptAll) {
            fingerPrintAcceptor = new AcceptAllHostKeyRepository();
        } else if (fingerPrint != null) {
            try {
                fingerPrintAcceptor = new FingerPrintAcceptor(fingerPrint);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Can't initialize fingerprint checker", e);
            }
        }

        this.userInfo = new UserInfo() {
            @Override
            public String getPassphrase() {
                return null;
            }

            @Override
            public String getPassword() {
                return new String(uri.getPassword());
            }

            @Override
            public boolean promptPassword(String message) {
                return uri.getPassword() != null;
            }

            @Override
            public boolean promptPassphrase(String message) {
                return false;
            }

            @Override
            public boolean promptYesNo(String message) {
                if (log.isInfoEnabled()) {
                    log.info("Prompt: " + message);
                }

                return false;
            }

            @Override
            public void showMessage(String message) {
            }
        };

    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        validateSession();

        String file;
        if (StringUtils.startsWith(path, "/")) {
            file = path;
        } else {
            file = FilenameUtils.separatorsToUnix(FilenameUtils.concat(uri.getPath().substring(1), path));
        }

        if (StringUtils.isBlank(file)) {
            throw new IOException("Invalid target path");
        }

        try {
            Channel channel = session.openChannel("sftp");
            channel.connect();
            try {
                ChannelSftp c = (ChannelSftp) channel;

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Upload file to " + file);
                    }

                    int i = file.lastIndexOf('/');
                    if (i >= 0) {
                        String parent = file.substring(0, i);
                        if (!SaverUtils.isDir(c, parent)) {
                            SaverUtils.mkdirs(c, parent);
                        }
                    }

                    try (OutputStream os = compression.cover(new BufferedOutputStream(c.put(file, ChannelSftp.OVERWRITE)))) {
                        IOUtils.copy(data, os);

                        os.flush();
                    }
                } catch (SftpException e) {
                    throw new IOException("Can't upload file to " + file, e);
                } finally {
                    c.exit();
                }
            } finally {
                channel.disconnect();
            }
        } catch (JSchException e) {
            throw new IOException(
                    "Failed to save data to sftp://" + uri.getHost() + (uri.getPort() == -1 ? "" : (":" + uri.getPort())) + "/" + file, e
            );
        }
    }

    @Override
    public void close() throws IOException {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private void validateSession() throws IOException {
        if (session != null && session.isConnected()) {
            try {
                // Ping
                ChannelExec testChannel = (ChannelExec) session.openChannel("exec");
                testChannel.setCommand("true");
                testChannel.connect();
                testChannel.disconnect();
                return;
            } catch (JSchException e) {
                // Ignore
            }
        }

        try {
            JSch jsch = new JSch();

            if (privateKeyFile != null) {
                jsch.addIdentity(privateKeyFile, passphrase);
            }

            if (fingerPrintAcceptor != null) {
                jsch.setHostKeyRepository(fingerPrintAcceptor);
            }

            if (uri.getPort() >= 0) {
                session = jsch.getSession(uri.getUser(), uri.getHost(), uri.getPort());
            } else {
                session = jsch.getSession(uri.getUser(), uri.getHost());
            }
            session.setUserInfo(userInfo);

            session.connect();
        } catch (JSchException e) {
            throw new IOException("Failed to establish a SFTP session", e);
        }
    }

}
