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
import java.util.regex.Matcher;

/**
 * 18.06.2014 18:20
 *
 * @author xBlackCat
 */
class SftpLocation implements ILocation {
    private static final Log log = LogFactory.getLog(SftpSaver.class);

    private final UserInfo userInfo;
    private final String host;
    private final int port;
    private byte[] passphrase;
    private HostKeyRepository fingerPrintAcceptor;
    private String privateKeyFile;
    private String userName;
    private String path;

    private Session session;

    public SftpLocation(URI basePath) throws IOException {
        host = basePath.getHost();
        port = basePath.getPort();

        final String pass;
        if (basePath.getRawUserInfo().indexOf(';') < 0) {
            String[] userInfo = StringUtils.split(basePath.getRawUserInfo(), ':');

            userName = SaverUtils.decode(userInfo[0]);
            pass = (userInfo.length == 1 || StringUtils.isEmpty(userInfo[1])) ? null : SaverUtils.decode(userInfo[1]);
        } else {
            String[] info = StringUtils.split(basePath.getRawUserInfo(), ';');

            privateKeyFile = null;
            String privateKeyPass = null;
            byte[] fingerPrint = null;
            boolean acceptAll = false;

            String[] params = StringUtils.split(info[1], ',');

            if (params.length > 0) {
                String lastItem = params[params.length - 1];
                int lastIndexOf = lastItem.lastIndexOf(':');
                if (lastIndexOf >= 0) {
                    // Possibly password is present
                    params[params.length - 1] = lastItem.substring(0, lastIndexOf);
                    pass = lastItem.substring(lastIndexOf + 1);
                } else {
                    pass = null;
                }
            } else {
                pass = null;
            }

            for (String param : params) {
                if ("accept-all".equalsIgnoreCase(param)) {
                    acceptAll = true;
                    continue;
                }
                final Matcher m = SaverUtils.PARAM_FETCHER.matcher(param);
                if (!m.matches()) {
                    if (log.isWarnEnabled()) {
                        log.warn("Invalid parameter definition '" + param + "' in uri " + basePath);
                    }
                    continue;
                }

                String name = m.group(1);
                String value = SaverUtils.decode(m.group(2));

                if ("fingerprint".equalsIgnoreCase(name)) {
                    String hexString = StringUtils.remove(value, '-');
                    fingerPrint = new byte[16];
                    ByteBuffer bb = ByteBuffer.wrap(fingerPrint);
                    bb.putInt((int) Long.parseLong(hexString.substring(0, 8), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(8, 16), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(16, 24), 16));
                    bb.putInt((int) Long.parseLong(hexString.substring(24, 32), 16));
                } else if ("private-key-file".equalsIgnoreCase(name)) {
                    privateKeyFile = value;
                } else if ("pk-passphrase".equalsIgnoreCase(name)) {
                    privateKeyPass = value;
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

            userName = SaverUtils.decode(info[0]);
        }

        this.userInfo = new UserInfo() {
            @Override
            public String getPassphrase() {
                return null;
            }

            @Override
            public String getPassword() {
                return pass;
            }

            @Override
            public boolean promptPassword(String message) {
                return pass != null;
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

        path = basePath.getPath().substring(1);
    }

    @Override
    public void save(String path, InputStream data, Compression compression) throws IOException {
        validateSession();

        String file;
        if (StringUtils.startsWith(path, "/")) {
            file = path;
        } else {
            file = FilenameUtils.separatorsToUnix(FilenameUtils.concat(this.path, path));
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
            throw new IOException("Failed to save data to sftp://" + host + (port == -1 ? "" : (":" + port)) + "/" + file, e);
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

            if (port >= 0) {
                session = jsch.getSession(userName, host, port);
            } else {
                session = jsch.getSession(userName, host);
            }
            session.setUserInfo(userInfo);

            session.connect();
        } catch (JSchException e) {
            throw new IOException("Failed to establish a SFTP session", e);
        }
    }

}
