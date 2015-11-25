package org.xblackcat.sjpu.saver;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.xblackcat.sjpu.utils.ParsedUri;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;

/**
 * 18.06.2014 17:33
 *
 * @author xBlackCat
 */
public class FtpsLocation extends AFtpLocation<FTPSClient> {
    public FtpsLocation(URI target) throws IOException {
        super(target);
    }

    @Override
     protected FTPSClient buildClient(ParsedUri.Param[] params) {
        X509TrustManager trustManager = TrustManagerUtils.getValidateServerCertificateTrustManager();

        boolean isImplicit = false;
        for (ParsedUri.Param param : params) {
            String name = param.getName();

            switch (name.toLowerCase()) {
                case "accept-all":
                    trustManager = TrustManagerUtils.getAcceptAllTrustManager();
                    break;
                case "validate":
                    trustManager = TrustManagerUtils.getValidateServerCertificateTrustManager();
                    break;
                case "no-validate":
                    trustManager = null;
                    break;
                case "implicit":
                    isImplicit = true;
                    break;
                case "explicit":
                    isImplicit = false;
                    break;
            }
        }

        FTPSClient ftpsClient = new FTPSClient(isImplicit);
        ftpsClient.setTrustManager(trustManager);
        return ftpsClient;
    }

    @Override
    protected void prepareToTransfer(FTPSClient ftpClient) throws IOException {
        super.prepareToTransfer(ftpClient);
        ftpClient.execPBSZ(0);
        ftpClient.execPROT("P");
    }
}
