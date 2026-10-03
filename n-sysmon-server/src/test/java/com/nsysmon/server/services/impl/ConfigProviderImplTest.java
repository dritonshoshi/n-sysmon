package com.nsysmon.server.services.impl;

import com.nsysmon.server.services.ConfigData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigProviderImplTest {
    @Test void providesBufferSizesAndWorkerThreadCounts() {
        final ConfigData underTest = new ConfigProviderImpl().getConfigData();

        assertEquals(1000, underTest.getEnvironmentQueueSize());
        assertEquals(10_000, underTest.getScalarQueueSize());
        assertEquals(1000, underTest.getTraceQueueSize());
    }

    @Test void providesAtLeastOneWorkerThreadPerDataType() {
        final ConfigData underTest = new ConfigProviderImpl().getConfigData();

        assertEquals(4, underTest.getNumEnvironmentWorkerThreads());
        assertEquals(10, underTest.getNumScalarWorkerThreads());
        assertEquals(10, underTest.getNumTraceWorkerThreads());
    }
}