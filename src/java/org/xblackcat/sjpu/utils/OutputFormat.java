package org.xblackcat.sjpu.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 01.04.2015 18:20
 *
 * @author xBlackCat
 */
public class OutputFormat {
    public static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    public static final byte[] UTF16LE_BOM = new byte[]{(byte) 0xFF, (byte) 0xFE};
    public static final byte[] UTF16BE_BOM = new byte[]{(byte) 0xFF, (byte) 0xFF};

    public static final OutputFormat UTF8_With_BOM =
            new OutputFormat(StandardCharsets.UTF_8, UTF8_BOM);
    public static final OutputFormat UTF8_WO_BOM =
            new OutputFormat(StandardCharsets.UTF_8, null);
    public static final OutputFormat UTF16LE_With_BOM =
            new OutputFormat(StandardCharsets.UTF_16LE, UTF16LE_BOM);
    public static final OutputFormat UTF16LE_WO_BOM =
            new OutputFormat(StandardCharsets.UTF_16LE, null);
    public static final OutputFormat UTF16BE_With_BOM =
            new OutputFormat(StandardCharsets.UTF_16BE, UTF16BE_BOM);
    public static final OutputFormat UTF16BE_WO_BOM =
            new OutputFormat(StandardCharsets.UTF_16BE, null);

    private final Charset outCharset;
    private final byte[] prefix;

    public OutputFormat(Charset outCharset, byte[] bomPrefix) {
        this.outCharset = outCharset;
        this.prefix = bomPrefix;
    }

    public byte[] getPrefix() {
        return prefix;
    }

    public Charset getOutCharset() {
        return outCharset;
    }

    public InputStream asInputStream(String content) {
        if (content == null) {
            throw new NullPointerException("Content is null");
        }
        final ByteArrayInputStream outStream = new ByteArrayInputStream(content.getBytes(getOutCharset()));
        if (getPrefix() == null || getPrefix().length == 0) {
            return outStream;
        }

        return new SequenceInputStream(new ByteArrayInputStream(getPrefix()), outStream);
    }
}
