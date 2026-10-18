package com.nsysmon.demo;

import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionConfig;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.server.Handler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Asserts the compression configuration without starting a server, so that a change to the mime type
 * list or to the minimum size is caught here rather than in the demo's own output.
 */
class DemoCompressionHandlerTest {

    private final Handler wrapped = mock(Handler.class);
    private final CompressionHandler handler = DemoCompressionHandler.createFor(wrapped);

    @Test void wrapsTheHandlerItIsGiven() {
        assertEquals(wrapped, handler.getHandler());
    }

    @Test void compressesExactlyTheConfiguredMimeTypes() {
        final CompressionConfig config = handler.getConfiguration("/");
        assertNotNull(config);

        for(String mimeType: DemoCompressionHandler.COMPRESSED_MIME_TYPES) {
            assertTrue(config.isCompressMimeTypeSupported(mimeType), mimeType + " is expected to be compressed");
        }
        assertFalse(config.isCompressMimeTypeSupported("image/png"));
        assertFalse(config.isCompressMimeTypeSupported("application/zip"));
    }

    @Test void keepsJettysDefaultsForTheHttpMethods() {
        final CompressionConfig config = handler.getConfiguration("/");

        assertTrue(config.isCompressMethodSupported("GET"));
        assertTrue(config.isCompressMethodSupported("POST"));
        assertFalse(config.isCompressMethodSupported("DELETE"));
    }

    @Test void registersGzipWithTheConfiguredMinimumSize() {
        final GzipCompression gzip = handler.getBeans().stream()
                .filter(GzipCompression.class::isInstance)
                .map(GzipCompression.class::cast)
                .findFirst()
                .orElse(null);

        assertNotNull(gzip, "the handler must register gzip itself, it must not rely on ServiceLoader discovery");
        assertEquals(DemoCompressionHandler.MIN_COMPRESS_SIZE, gzip.getMinCompressSize());
    }
}
