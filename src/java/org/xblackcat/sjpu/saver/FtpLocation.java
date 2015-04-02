package org.xblackcat.sjpu.saver;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.net.URI;

/**
 * 18.06.2014 17:33
 *
 * @author xBlackCat
 */
class FtpLocation extends AFtpLocation<FTPClient> {
    FtpLocation(URI target) throws IOException {
        super(target);
    }

    @Override
    protected FTPClient buildClient(ParsedUri.Param[] params) {
        return new FTPClient();
    }
}
