package com.nsysmon.server.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScalarMetaDataTest {
    @Test void exposesNameAndNumberOfFractionDigits() {
        final ScalarMetaData underTest = new ScalarMetaData("cpu.load", 3);

        assertEquals("cpu.load", underTest.name);
        assertEquals(3, underTest.numFracDigits);
    }

    @Test void allowsZeroFractionDigits() {
        assertEquals(0, new ScalarMetaData("heap.used", 0).numFracDigits);
    }
}