package com.nsysmon.demo;

import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.server.Handler;

import java.util.Arrays;
import java.util.List;

/**
 * The response compression of the demo.
 * <p>
 * Uses the compression API that replaced the deprecated
 * {@code org.eclipse.jetty.server.handler.gzip.GzipHandler} (deprecated since Jetty 12.1.1, marked
 * for removal). The gzip implementation is registered explicitly rather than left to the
 * {@code ServiceLoader} discovery in {@link CompressionHandler#doStart()}: the webapp runs in its
 * own classloader, so a discovery based on the thread context classloader may not see
 * {@code org.eclipse.jetty.compression:jetty-compression-gzip} and compression would silently be
 * skipped.
 *
 * @author arno
 */
final class DemoCompressionHandler {
    static final int MIN_COMPRESS_SIZE = 256;

    /**
     * The mime types whose responses are compressed. The include list is exhaustive: anything not
     * listed here is passed through uncompressed.
     */
    static final List<String> COMPRESSED_MIME_TYPES = Arrays.asList("text/html", "text/plain", "text/xml",
            "text/css", "application/javascript", "application/json");

    private DemoCompressionHandler() {
        // static helper
    }

    static CompressionHandler createFor(Handler handler) {
        final CompressionHandler result = new CompressionHandler(handler);
        result.putCompression(gzip());
        result.putConfiguration("/", config());

        return result;
    }

    private static GzipCompression gzip() {
        final GzipCompression result = new GzipCompression();
        result.setMinCompressSize(MIN_COMPRESS_SIZE);

        return result;
    }

    private static CompressionConfig config() {
        // the defaults compress GET and POST responses, leave content that is already compressed
        // alone and decompress gzipped POST request bodies
        CompressionConfig.Builder builder = CompressionConfig.builder().defaults();
        for(String mimeType: COMPRESSED_MIME_TYPES) {
            builder = builder.compressIncludeMimeType(mimeType);
        }

        return builder.build();
    }
}
