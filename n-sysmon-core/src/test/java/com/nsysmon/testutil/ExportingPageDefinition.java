package com.nsysmon.testutil;

import com.nsysmon.servlet.overview.DataFileGeneratorSupporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A page definition that can export itself as a data file, writing a fixed payload so that tests can verify it.
 */
public class ExportingPageDefinition extends StubPageDefinition implements DataFileGeneratorSupporter {
    public static final String PAYLOAD = "{\"exported\":true}";

    private static final AtomicInteger NUM_EXPORTS = new AtomicInteger();

    public ExportingPageDefinition(String id) {
        super(id);
    }

    public static int numExports() {
        return NUM_EXPORTS.get();
    }

    public static void resetNumExports() {
        NUM_EXPORTS.set(0);
    }

    @Override public void getDataForExport(OutputStream os) throws IOException {
        NUM_EXPORTS.incrementAndGet();
        final ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.write(PAYLOAD.getBytes(StandardCharsets.UTF_8));
        content.writeTo(os);
    }
}
