package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AEnvVarJdkParameterMeasurerTest {

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AEnvVarJdkParameterMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AEnvVarJdkParameterMeasurer();
    }

    private static boolean isAllowedInCleartext(String envName) throws Exception {
        final Method method = AEnvVarJdkParameterMeasurer.class.getDeclaredMethod("isAllowedInCleartext", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(new AEnvVarJdkParameterMeasurer(), envName);
    }

    @Test
    public void everyEnvironmentVariableIsCollectedUnderTheJdkParamKey() throws Exception {
        measurer.contributeMeasurements(collector);

        assertEquals(System.getenv().size(), collected.size());
        assertTrue(collected.stream().allMatch(d ->
                AEnvVarJdkParameterMeasurer.KEY_ENV_VAR.equals(d.getName().asJavaUtilList().get(0))));
    }

    @Test
    public void regularVariablesAreCollectedInCleartext() throws Exception {
        measurer.contributeMeasurements(collector);

        final String name = System.getenv().keySet().iterator().next();
        final AEnvironmentData data = collected.stream()
                .filter(d -> name.equals(d.getName().asJavaUtilList().get(1)))
                .findFirst().orElseThrow();

        assertEquals(System.getenv(name), data.getValue());
    }

    @Test
    public void secretLookingVariableNamesAreMasked() throws Exception {
        assertFalse(isAllowedInCleartext("SECURE_X"));
        assertFalse(isAllowedInCleartext("MY_PASSWORD"));
        assertFalse(isAllowedInCleartext("my.truststore.path"));
        assertFalse(isAllowedInCleartext("my_keystore_path"));
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        measurer.contributeMeasurements(collector);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }

    @Test
    public void unrelatedVariableNamesAreNotMasked() throws Exception {
        assertTrue(isAllowedInCleartext("HOME"));
    }
}