package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.IOUtils;
import org.xblackcat.sjpu.utils.UriUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;

/**
 * 27.02.13 17:52
 *
 * @author xBlackCat
 */
public class SftpSaver implements ISaver {
    private static final Log log = LogFactory.getLog(SftpSaver.class);

    @Override
    public void save(final URI target, InputStream data, Compression compression) throws IOException {
        JSch jsch = new JSch();

        try {
            String[] userInfo = StringUtils.split(target.getRawUserInfo(), ':');
            final String pass = (userInfo.length == 1 || StringUtils.isEmpty(userInfo[1])) ? null : SaverUtils.decode(
                    userInfo[1]
            );

            final String userName;
            if (userInfo[0].indexOf(';') < 0) {
                userName = SaverUtils.decode(userInfo[0]);
            } else {
                String[] info = StringUtils.split(userInfo[0], ';');

                String privateKeyFile = null;
                String privateKeyPass = null;
                byte[] fingerPrint = null;
                boolean acceptAll = false;

                String[] params = StringUtils.split(info[1], ',');
                for (String param : params) {
                    if ("accept-all".equalsIgnoreCase(param)) {
                        acceptAll = true;
                        continue;
                    }
                    final Matcher m = SaverUtils.PARAM_FETCHER.matcher(param);
                    if (!m.matches()) {
                        if (log.isWarnEnabled()) {
                            log.warn("Invalid parameter definition '" + param + "' in uri " + target);
                        }
                        continue;
                    }

                    String name = m.group(1);
                    String value = SaverUtils.decode(m.group(2));

                    if ("fingerprint".equalsIgnoreCase(name)) {
                        fingerPrint = new BigInteger(StringUtils.remove(value, '-'), 16).toByteArray();
                    } else if ("private-key-file".equalsIgnoreCase(name)) {
                        privateKeyFile = value;
                    } else if ("pk-passphrase".equalsIgnoreCase(name)) {
                        privateKeyPass = value;
                    }
                }

                if (privateKeyFile != null) {
                    final byte[] passphrase = privateKeyPass !=
                            null ? privateKeyPass.getBytes(StandardCharsets.UTF_8) : null;

                    jsch.addIdentity(privateKeyFile, passphrase);
                }

                if (acceptAll) {
                    jsch.setHostKeyRepository(new AcceptAllHostKeyRepository());
                } else if (fingerPrint != null) {
                    try {
                        jsch.setHostKeyRepository(new FingerPrintAcceptor(fingerPrint));
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException("Can't initialize fingerprint checker", e);
                    }
                }

                userName = SaverUtils.decode(info[0]);
            }

            final Session session;
            if (target.getPort() >= 0) {
                session = jsch.getSession(userName, target.getHost(), target.getPort());
            } else {
                session = jsch.getSession(userName, target.getHost());
            }
            session.setUserInfo(
                    new UserInfo() {
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
                    }
            );

            session.connect();

            try {
                Channel channel = session.openChannel("sftp");
                channel.connect();
                try {
                    ChannelSftp c = (ChannelSftp) channel;

                    try {
                        final String path = target.getPath().substring(1);
                        if (log.isDebugEnabled()) {
                            log.debug("Upload file to " + path);
                        }

                        int i = path.lastIndexOf('/');
                        if (i >= 0) {
                            String parent = path.substring(0, i);
                            if (!SaverUtils.isDir(c, parent)) {
                                SaverUtils.mkdirs(c, parent);
                            }
                        }

                        try (OutputStream os = compression.cover(new BufferedOutputStream(c.put(path, ChannelSftp.OVERWRITE)))) {
                            IOUtils.copy(data, os);

                            os.flush();
                        }
                    } catch (SftpException e) {
                        throw new IOException("Can't upload file to " + target, e);
                    } finally {
                        c.exit();
                    }
                } finally {
                    channel.disconnect();
                }
            } finally {
                session.disconnect();
            }
        } catch (JSchException e) {
            throw new IOException("Can't connect to " + UriUtils.toString(target), e);
        }
    }

}
