package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomValues2Test {

    private static final long TIMESTAMP = 1_700_000_000_000L;

    private final RandomValues2 measurer = new RandomValues2();

    private static Map<String, AScalarDataPoint> collect(RandomValues2 measurer) throws Exception {
        final Map<String, AScalarDataPoint> data = new HashMap<>();
        measurer.contributeMeasurements(data, TIMESTAMP, new HashMap<>());
        return data;
    }

    @Test void contributeMeasurementsPublishesARandomNumber() throws Exception {
        final AScalarDataPoint dataPoint = collect(measurer).get(RandomValues2.MEASUREMENT_NAME);

        assertNotNull(dataPoint);
        assertEquals(RandomValues2.MEASUREMENT_NAME, dataPoint.getName());
        assertEquals(TIMESTAMP, dataPoint.getTimestamp());
        assertEquals(0, dataPoint.getNumFracDigits());
    }

    @Test void contributedValuesStayWithinTheDocumentedRange() throws Exception {
        for(int i = 0; i < 100; i++) {
            final long value = collect(measurer).get(RandomValues2.MEASUREMENT_NAME).getValueRaw();
            assertTrue(value >= 0 && value < 100, "value out of range: " + value);
        }
    }

    @Test void prepareMeasurementsStoresNoMemento() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);

        assertTrue(mementos.isEmpty());
    }

    @Test void shutdownDoesNothing() throws Exception {
        measurer.shutdown();
    }

    @Test void thereIsNoTimeoutConfiguration() {
        final AOption<Long> timeout = measurer.getTimeoutInMilliSeconds();

        assertTrue(timeout.isEmpty());
    }

    @Test void ownMeasurementIsGroupedAsTest() {
        assertEquals("Test", measurer.getGroupnameOfMeasurement(RandomValues2.MEASUREMENT_NAME));
        assertEquals("Test", measurer.getGroupnameOfMeasurement("RANDOMNUMBER 2"));
    }

    @Test void foreignMeasurementsHaveNoGroup() {
        assertNull(measurer.getGroupnameOfMeasurement(RandomValues.MEASUREMENT_NAME));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test void ownMeasurementHasADescription() {
        assertEquals("Generates some more random values.", measurer.getDescriptionOfMeasurement(RandomValues2.MEASUREMENT_NAME));
    }

    @Test void foreignMeasurementsHaveNoDescription() {
        assertNull(measurer.getDescriptionOfMeasurement(RandomValues.MEASUREMENT_NAME));
    }

    @Test void responsibilityFollowsTheDescription() {
        assertTrue(measurer.isResponsibleForMeasurement(RandomValues2.MEASUREMENT_NAME));
        assertFalse(measurer.isResponsibleForMeasurement("something else"));
    }
}