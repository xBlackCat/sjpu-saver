package org.xblackcat.sjpu.saver;

import org.apache.commons.net.ftp.FTPClient;
import org.xblackcat.sjpu.utils.ParsedUri;

import java.io.IOException;
import java.net.URI;

/**
 * 18.06.2014 17:33
 *
 * @author xBlackCat
 */
public class FtpLocation extends AFtpLocation<FTPClient> {
    public FtpLocation(URI target) throws IOException {
        super(target);
    }

    @Override
    protected FTPClient buildClient(ParsedUri.Param[] params) {
        return new FTPClient();
    }
}
