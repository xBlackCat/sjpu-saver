package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.FilenameUtils;
import org.xblackcat.sjpu.utils.IOUtils;
import org.xblackcat.sjpu.utils.ParsedUri;

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
public class SftpLocation implements ILocation {
    private static final Log log = LogFactory.getLog(SftpLocation.class);

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
            passphrase = privateKeyPass != null && privateKeyPass.length() > 0 ? privateKeyPass.getBytes(StandardCharsets.UTF_8) : null;
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

        path = path.substring(1);

        String file;
        if (StringUtils.startsWith(path, "/")) {
            file = path;
        } else {
            file = FilenameUtils.concat(uri.getPath().substring(1), path);
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

                    int i = path.lastIndexOf('/');
                    if (i > 0) {
                        mkdirs(c, path.substring(0, i));
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
    public void close() {
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

    private static void mkdirs(ChannelSftp channel, String path) throws SftpException {
        if (log.isTraceEnabled()) {
            log.trace("Touch dir " + path);
        }

        if (path.length() == 0 || "/".equals(path)) {
            return;
        }
        Status status = isDir(channel, path);
        if (status == Status.NotExists) {
            int i = path.lastIndexOf('/');
            if (i >= 0) {
                mkdirs(channel, path.substring(0, i));
            }
            if (log.isTraceEnabled()) {
                log.trace("Create dir " + path);
            }

            channel.mkdir(path);
        } else if (status == Status.File) {
            throw new SftpException(ChannelSftp.SSH_FX_FAILURE, "Expected dir on path " + path + " but got file");
        }
    }

    private static Status isDir(ChannelSftp channel, String parentPath) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Check parent folder for existence: " + parentPath);
            }
            SftpATTRS stat = channel.stat(parentPath);
            if (stat != null) {
                return stat.isDir() ? Status.Directoty : Status.File;
            }
        } catch (SftpException e) {
            // Not found
            if (log.isTraceEnabled()) {
                log.trace("Folder " + parentPath + " is not exists.");
            }
        }
        return Status.NotExists;
    }

    private enum Status {
        NotExists,
        Directoty,
        File
    }
}
