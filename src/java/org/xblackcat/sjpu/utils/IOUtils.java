package org.xblackcat.sjpu.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOUtils {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private IOUtils() {
    }

    /**
     * Copies the content of a InputStream into an OutputStream.
     * Uses a default buffer size of 8024 bytes.
     *
     * @param input  the InputStream to copy
     * @param output the target Stream
     * @return amount of copied bytes
     * @throws IOException if an error occurs
     */
    public static long copy(final InputStream input, final OutputStream output) throws IOException {
        return copy(input, output, 8024);
    }

    /**
     * Copies the content of a InputStream into an OutputStream
     *
     * @param input      the InputStream to copy
     * @param output     the target Stream
     * @param buffersize the buffer size to use
     * @return amount of copied bytes
     * @throws IOException if an error occurs
     */
    public static long copy(final InputStream input, final OutputStream output, int buffersize) throws IOException {
        final byte[] buffer = new byte[buffersize];
        int n;
        long count = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
