package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xblackcat.sjpu.utils.IOUtils;
import org.xblackcat.sjpu.utils.UriUtils;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 27.02.13 17:52
 *
 * @author xBlackCat
 */
public class SftpSaver implements ISaver {
    private static final Log log = LogFactory.getLog(SftpSaver.class);

    private final static Pattern PARAM_FETCHER = Pattern.compile("([-\\w]*)=(.*)");

    @Override
    public void save(final URI target, InputStream data, Compression compression) throws IOException {
        JSch jsch = new JSch();

        try {
            String[] userInfo = StringUtils.split(target.getRawUserInfo(), ':');
            final String pass = (userInfo.length == 1 || StringUtils.isEmpty(userInfo[1])) ? null : decode(userInfo[1]);

            final String userName;
            if (userInfo[0].indexOf(';') < 0) {
                userName = decode(userInfo[0]);
            } else {
                String[] info = StringUtils.split(userInfo[0], ';');

                String privateKeyFile = null;
                String privateKeyPass = null;
                byte[] fingerPrint = null;

                String[] params = StringUtils.split(info[1], ',');
                for (String param : params) {
                    final Matcher m = PARAM_FETCHER.matcher(param);
                    if (!m.matches()) {
                        if (log.isWarnEnabled()) {
                            log.warn("Invalid parameter definition '" + param + "' in uri " + target);
                        }
                        continue;
                    }

                    String name = m.group(1);
                    String value = decode(m.group(2));

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

                if (fingerPrint != null) {
                    try {
                        jsch.setHostKeyRepository(new FingerPrintAcceptor(fingerPrint));
                    } catch (NoSuchAlgorithmException e) {
                        throw new IOException("Can't initialize fingerprint checker", e);
                    }
                }

                userName = decode(info[0]);
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
                            if (!isDir(c, parent)) {
                                mkdirs(c, parent);
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

    private static void mkdirs(ChannelSftp channel, String path) throws SftpException {

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

    private static boolean isDir(ChannelSftp channel, String parentPath) {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Check parent folder for existence: " + parentPath);
            }
            SftpATTRS stat = channel.stat(parentPath);
            return stat != null && stat.isDir();
        } catch (SftpException e) {
            // Not found
            if (log.isTraceEnabled()) {
                log.trace("Folder " + parentPath + " is not exists.", e);
            }
            return false;
        }

    }

    private static String decode(String s) throws UnsupportedEncodingException {
        return URLDecoder.decode(s, "UTF-8");
    }

    private static class FingerPrintAcceptor implements HostKeyRepository {

        private final MessageDigest md5;
        private final byte[] fingerPrint;

        public FingerPrintAcceptor(byte[] fingerPrint) throws NoSuchAlgorithmException {
            this.fingerPrint = fingerPrint.clone();
            md5 = MessageDigest.getInstance("MD5");
        }

        @Override
        public int check(String host, byte[] data) {
            md5.update(data, 0, data.length);
            byte[] foo = md5.digest();

            return Arrays.equals(fingerPrint, foo) ? OK : NOT_INCLUDED;
        }

        @Override
        public void add(HostKey hostkey, UserInfo ui) {
        }

        @Override
        public void remove(String host, String type) {
        }

        @Override
        public void remove(String host, String type, byte[] key) {
        }

        @Override
        public String getKnownHostsRepositoryID() {
            return null;
        }

        @Override
        public HostKey[] getHostKey() {
            return new HostKey[0];
        }

        @Override
        public HostKey[] getHostKey(String host, String type) {
            return getHostKey();
        }
    }
}
