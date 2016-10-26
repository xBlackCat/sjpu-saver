package org.xblackcat.sjpu.saver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 27.02.13 17:07
 *
 * @author xBlackCat
 */
public enum Compression {
    Plain {
        @Override
        public OutputStream cover(OutputStream stream) {
            return stream;
        }

        @Override
        public InputStream cover(InputStream stream) throws IOException {
            return stream;
        }
    },
    GZip {
        @Override
        public OutputStream cover(OutputStream stream) throws IOException {
            return new GZIPOutputStream(stream);
        }

        @Override
        public InputStream cover(InputStream stream) throws IOException {
            return new GZIPInputStream(stream);
        }
    },
    XZ {
        @Override
        public OutputStream cover(OutputStream stream) throws IOException {
            try {
                if (XZCompressor != null) {
                    return XZCompressor.newInstance(stream, 7);
                }
            } catch (Exception e) {
                throw new IOException("Can't build XZ compressor", e);
            }

            throw new IOException(
                    "XZ engine is not initialized. Check if commons-compress.jar and xz.jar(org.tukaani.xz) libraries are in the runtime classpath"
            );
        }

        @Override
        public InputStream cover(InputStream stream) throws IOException {
            try {
                if (XZDecompressor != null) {
                    return XZDecompressor.newInstance(stream);
                }
            } catch (Exception e) {
                throw new IOException("Can't build XZ decompressor", e);
            }

            throw new IOException(
                    "XZ engine is not initialized. Check if commons-compress.jar and xz.jar(org.tukaani.xz) libraries are in the runtime classpath"
            );
        }
    },
    BZip2 {
        @Override
        public OutputStream cover(OutputStream stream) throws IOException {
            try {
                if (BZip2Compressor != null) {
                    return BZip2Compressor.newInstance(stream, 9);
                }
            } catch (Exception e) {
                throw new IOException("Can't build BZip2 compressor", e);
            }

            throw new IOException("BZip2 engine is not initialized. Check if a commons-compress.jar library is in the runtime classpath");
        }

        @Override
        public InputStream cover(InputStream stream) throws IOException {
            try {
                if (BZip2Decompressor != null) {
                    return BZip2Decompressor.newInstance(stream);
                }
            } catch (Exception e) {
                throw new IOException("Can't build BZip2 decompressor", e);
            }

            throw new IOException("BZip2 engine is not initialized. Check if a commons-compress.jar library is in the runtime classpath");
        }
    };

    private static final Log log = LogFactory.getLog(Compression.class);

    private static final Constructor<? extends OutputStream> BZip2Compressor;
    private static final Constructor<? extends OutputStream> XZCompressor;
    private static final Constructor<? extends InputStream> BZip2Decompressor;
    private static final Constructor<? extends InputStream> XZDecompressor;

    static {
        BZip2Compressor = initializeConstructor(
                OutputStream.class,
                "org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream",
                int.class
        );
        XZCompressor = initializeConstructor(
                OutputStream.class,
                "org.apache.commons.compress.compressors.xz.XZCompressorOutputStream",
                int.class
        );
        BZip2Decompressor = initializeConstructor(
                InputStream.class,
                "org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream"
        );
        XZDecompressor = initializeConstructor(
                InputStream.class,
                "org.apache.commons.compress.compressors.xz.XZCompressorInputStream"
        );
    }

    private static <Base, Target extends Base> Constructor<Target> initializeConstructor(
            Class<Base> targetStream,
            String className,
            Class<?>... additionalArgs
    ) {
        Constructor<Target> constructor;
        try {
            final Class<?>[] args;
            if (additionalArgs == null || additionalArgs.length == 0) {
                args = new Class[]{targetStream};
            } else {
                args = new Class[additionalArgs.length + 1];
                args[0] = targetStream;
                System.arraycopy(additionalArgs, 0, args, 1, additionalArgs.length);
            }
            @SuppressWarnings("unchecked")
            final Class<Target> aClass = (Class<Target>) Class.forName(className);
            constructor = aClass.getConstructor(args);
        } catch (ReflectiveOperationException | LinkageError e) {
            constructor = null;
            log.trace("Can't initialize compressor " + className, e);
        }

        return constructor;
    }

    public abstract OutputStream cover(OutputStream stream) throws IOException;

    public abstract InputStream cover(InputStream stream) throws IOException;
}
