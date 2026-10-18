package com.nsysmon.testutil;

import jakarta.servlet.ServletOutputStream;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A {@link ServletOutputStream} that buffers everything written to it, so that tests can inspect the response body
 *  without a servlet container.
 */
public class RecordingServletOutputStream extends ServletOutputStream {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override public boolean isReady() {
        return true;
    }

    @Override public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
    }

    @Override public void write(int b) {
        buffer.write(b);
    }

    public String content() {
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
