package com.nsysmon.testutil;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Captures the JSON that the page definitions write, so that tests can assert on the produced document.
 */
public class RecordingJson {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final AJsonSerHelperForNSysmon helper = new AJsonSerHelperForNSysmon(buffer);

    public AJsonSerHelperForNSysmon helper() {
        return helper;
    }

    public String json() {
        try {
            helper.getOut().flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * The helper writes a newline after every closing brace, which makes expected values in tests awkward to read.
     */
    public String compactJson() {
        return json().replace("\n", "");
    }
}
