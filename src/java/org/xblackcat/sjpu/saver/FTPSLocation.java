package org.xblackcat.sjpu.saver;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.net.URI;

/**
 * 18.06.2014 17:33
 *
 * @author xBlackCat
 */
class FTPSLocation extends AFtpLocation<FTPClient> {
    FTPSLocation(URI target) throws IOException {
        super(target, new FTPClient());
    }

}
