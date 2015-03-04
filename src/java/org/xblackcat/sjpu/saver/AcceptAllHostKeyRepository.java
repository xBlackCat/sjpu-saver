package org.xblackcat.sjpu.saver;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.UserInfo;

/**
 * 04.03.2015 12:16
 *
 * @author xBlackCat
 */
class AcceptAllHostKeyRepository implements HostKeyRepository {
    @Override
    public int check(String host, byte[] key) {
        return OK;
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
        return new HostKey[0];
    }
}
