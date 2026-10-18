package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AEnvVarEnvironmentMeasurerTest {

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AEnvVarEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AEnvVarEnvironmentMeasurer();
    }

    private static boolean isAllowedInCleartext(String envName) throws Exception {
        final Method method = AEnvVarEnvironmentMeasurer.class.getDeclaredMethod("isAllowedInCleartext", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(new AEnvVarEnvironmentMeasurer(), envName);
    }

    private List<String> names() {
        return collected.stream().map(d -> d.getName().asJavaUtilList().get(1)).toList();
    }

    @Test
    public void allEnvironmentVariablesAreCollected() throws Exception {
        measurer.contributeMeasurements(collector);

        assertEquals(System.getenv().size(), collected.size());
        assertTrue(names().containsAll(System.getenv().keySet()));
    }

    @Test
    public void regularVariablesAreCollectedInCleartext() throws Exception {
        measurer.contributeMeasurements(collector);

        final Map.Entry<String, String> first = System.getenv().entrySet().iterator().next();
        final AEnvironmentData data = collected.stream()
                .filter(d -> first.getKey().equals(d.getName().asJavaUtilList().get(1)))
                .findFirst().orElseThrow();

        assertEquals(first.getValue(), data.getValue());
    }

    @Test
    public void everyEntryIsStoredUnderTheEnvVarKey() throws Exception {
        measurer.contributeMeasurements(collector);

        assertTrue(collected.stream().allMatch(d ->
                AEnvVarEnvironmentMeasurer.KEY_ENV_VAR.equals(d.getName().asJavaUtilList().get(0))));
    }

    @Test
    public void variableNamesContainingPasswordAreMasked() throws Exception {
        assertFalse(isAllowedInCleartext("MY_PASSWORD"));
        assertFalse(isAllowedInCleartext("db.password.value"));
    }

    @Test
    public void variableNamesContainingSecureAreMasked() throws Exception {
        assertFalse(isAllowedInCleartext("SECURE_TOKEN"));
    }

    @Test
    public void variableNamesContainingTruststoreOrKeystoreAreMasked() throws Exception {
        assertFalse(isAllowedInCleartext("javax.net.ssl.trustStorePassword"));
        assertFalse(isAllowedInCleartext("KEYSTORE_LOCATION"));
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
        assertTrue(isAllowedInCleartext("PATH"));
        assertTrue(isAllowedInCleartext("my_plain_value"));
    }
}
