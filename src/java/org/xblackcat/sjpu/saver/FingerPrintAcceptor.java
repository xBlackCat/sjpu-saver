package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * 19.06.2014 09:40
 *
 * @author xBlackCat
 */
class FingerPrintAcceptor implements HostKeyRepository {

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
