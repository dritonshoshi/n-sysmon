package com.nsysmon.measure.scalar;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AScalarMeasurerTest {

    private static class MinimalMeasurer implements AScalarMeasurer {
        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        }
        @Override public AOption<Long> getTimeoutInMilliSeconds() {
            return AOption.some(1000L);
        }
        @Override public void shutdown() {
        }
        @Override public String getDescriptionOfMeasurement(String measurement) {
            return "known".equals(measurement) ? "a known measurement" : null;
        }
    }

    @Test
    public void prepareMeasurementsDoesNothingByDefault() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        new MinimalMeasurer().prepareMeasurements(mementos);

        assertTrue(mementos.isEmpty());
    }

    @Test
    public void groupnameIsNullByDefault() {
        assertNull(new MinimalMeasurer().getGroupnameOfMeasurement("known"));
    }

    @Test
    public void configurationParametersAreEmptyByDefault() {
        assertTrue(new MinimalMeasurer().getConfigurationParameters().isEmpty());
    }

    @Test
    public void measurerIsResponsibleOnlyForDescribedMeasurements() {
        final AScalarMeasurer measurer = new MinimalMeasurer();

        assertTrue(measurer.isResponsibleForMeasurement("known"));
        assertFalse(measurer.isResponsibleForMeasurement("unknown"));
    }

    @Test
    public void configurationKeysHaveTheExpectedNames() {
        assertEquals("active", AScalarMeasurer.KEY_CONFIGURATION_ACTIVE);
        assertEquals("medium", AScalarMeasurer.KEY_CONFIGURATION_MEDIUM);
        assertEquals("high", AScalarMeasurer.KEY_CONFIGURATION_HIGH);
    }

    @Test
    public void evaluatedValuesAreOrderedFromLowToHigh() {
        assertEquals(List.of(AScalarMeasurer.EvaluatedValue.LOW, AScalarMeasurer.EvaluatedValue.MEDIUM,
                AScalarMeasurer.EvaluatedValue.HIGH), List.of(AScalarMeasurer.EvaluatedValue.values()));
    }
}