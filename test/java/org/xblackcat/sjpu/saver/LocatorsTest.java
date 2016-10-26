package org.xblackcat.sjpu.saver;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * 30.09.2016 15:30
 *
 * @author xBlackCat
 */
public class LocatorsTest {
    @Test
    public void customerLocator() throws IOException {
        final URI uri = URI.create("dumb://path/file");

        try (final ISaver saver = new Saver()) {
            try (final InputStream is = new ByteArrayInputStream(new byte[0])) {
                saver.save(uri, is, Compression.Plain);
            }
        }
    }
}
