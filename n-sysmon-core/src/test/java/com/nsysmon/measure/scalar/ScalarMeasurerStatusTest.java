package com.nsysmon.measure.scalar;

import com.nsysmon.measure.scalar.ScalarMeasurerStatus.Status;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author arno
 */
public class ScalarMeasurerStatusTest {

    @Test
    public void statusIsReturnedUnchanged() {
        assertEquals(Status.RUNNING, new ScalarMeasurerStatus(Status.RUNNING).getStatus());
        assertEquals(Status.STOPPED, new ScalarMeasurerStatus(Status.STOPPED).getStatus());
        assertEquals(Status.UNKNOWN, new ScalarMeasurerStatus(Status.UNKNOWN).getStatus());
    }

    @Test
    public void allStatusesAreAvailable() {
        assertEquals(List.of(Status.RUNNING, Status.STOPPED, Status.UNKNOWN), List.of(Status.values()));
    }
}