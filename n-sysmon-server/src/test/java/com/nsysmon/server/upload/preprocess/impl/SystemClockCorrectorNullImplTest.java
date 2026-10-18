package com.nsysmon.server.upload.preprocess.impl;

import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemClockCorrectorNullImplTest {
    private static final InstanceIdentifier INSTANCE = Nodes.instance("app-1", "instance-1");

    private final SystemClockCorrectorNullImpl underTest = new SystemClockCorrectorNullImpl();

    @Test void correctedTimestampReturnsTheRawTimestampUnchanged() {
        assertEquals(1234567L, underTest.correctedTimestamp(INSTANCE, 1234567L));
    }

    @Test void correctedTimestampDoesNotDependOnTheInstance() {
        final long raw = 1234567L;

        assertEquals(underTest.correctedTimestamp(INSTANCE, raw),
                underTest.correctedTimestamp(Nodes.instance("other-app", "other-instance"), raw));
    }

    @Test void updateSystemClockDiffChangesNothing() {
        underTest.updateSystemClockDiff(INSTANCE, 1234567L);

        assertEquals(1234567L, underTest.correctedTimestamp(INSTANCE, 1234567L));
    }
}
