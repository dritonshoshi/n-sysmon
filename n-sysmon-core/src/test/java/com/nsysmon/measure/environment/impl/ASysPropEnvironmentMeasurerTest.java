package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class ASysPropEnvironmentMeasurerTest {

    static final String SECRET_PROP = "nsysmon.test.password";
    static final String PLAIN_PROP = "nsysmon.test.plain";

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private ASysPropEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new ASysPropEnvironmentMeasurer();
        System.setProperty(SECRET_PROP, "top-secret");
        System.setProperty(PLAIN_PROP, "top-public");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(SECRET_PROP);
        System.clearProperty(PLAIN_PROP);
    }

    static List<String> nameOf(AEnvironmentData data) {
        return List.copyOf(data.getName().asJavaUtilList());
    }

    private List<String> namesOf(String rootKey) {
        return collected.stream()
                .filter(d -> nameOf(d).size() == 2)
                .filter(d -> rootKey.equals(nameOf(d).get(0)))
                .map(d -> nameOf(d).get(1))
                .toList();
    }

    @Test
    public void allSystemPropertiesAreCollected() throws Exception {
        measurer.contributeMeasurements(collector);

        assertTrue(namesOf(ASysPropEnvironmentMeasurer.KEY_SYSTEM_PROPERTY).contains(PLAIN_PROP));
        assertTrue(namesOf(ASysPropEnvironmentMeasurer.KEY_SYSTEM_PROPERTY).contains("java.vendor"));
        assertEquals(System.getProperties().stringPropertyNames().size(), collected.size());
    }

    @Test
    public void regularPropertiesAreCollectedInCleartext() throws Exception {
        measurer.contributeMeasurements(collector);

        final AEnvironmentData plain = collected.stream()
                .filter(d -> nameOf(d).equals(List.of(ASysPropEnvironmentMeasurer.KEY_SYSTEM_PROPERTY, PLAIN_PROP)))
                .findFirst().orElseThrow();

        assertEquals("top-public", plain.getValue());
    }

    @Test
    public void passwordLikePropertiesAreMasked() throws Exception {
        measurer.contributeMeasurements(collector);

        final AEnvironmentData secret = collected.stream()
                .filter(d -> nameOf(d).equals(List.of(ASysPropEnvironmentMeasurer.KEY_SYSTEM_PROPERTY, SECRET_PROP)))
                .findFirst().orElseThrow();

        assertEquals("********", secret.getValue());
    }

    @Test
    public void everyEntryIsStoredUnderTheSysPropKey() throws Exception {
        measurer.contributeMeasurements(collector);

        assertNotNull(collected);
        assertTrue(collected.stream().allMatch(d -> ASysPropEnvironmentMeasurer.KEY_SYSTEM_PROPERTY.equals(nameOf(d).get(0))));
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        measurer.contributeMeasurements(collector);

        measurer.shutdown();

        assertEquals(System.getProperties().stringPropertyNames().size(), collected.size());
    }
}