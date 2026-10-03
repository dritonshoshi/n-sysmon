package com.nsysmon.server.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigDataTest {
    @Test void exposesQueueSizes() {
        final ConfigData underTest = new ConfigData(1000, 2000, 3000, 1, 2, 3);

        assertEquals(1000, underTest.getEnvironmentQueueSize());
        assertEquals(2000, underTest.getScalarQueueSize());
        assertEquals(3000, underTest.getTraceQueueSize());
    }

    @Test void exposesWorkerThreadCounts() {
        final ConfigData underTest = new ConfigData(1000, 2000, 3000, 1, 2, 3);

        assertEquals(1, underTest.getNumEnvironmentWorkerThreads());
        assertEquals(2, underTest.getNumScalarWorkerThreads());
        assertEquals(3, underTest.getNumTraceWorkerThreads());
    }
}