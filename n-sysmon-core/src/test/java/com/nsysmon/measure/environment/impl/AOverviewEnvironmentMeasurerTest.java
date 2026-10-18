package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class AOverviewEnvironmentMeasurerTest {

    private static final Path MEMINFO = Path.of("/proc/meminfo");

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AOverviewEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AOverviewEnvironmentMeasurer();
    }

    private String valueOf(String key) {
        final String path = AOverviewEnvironmentMeasurer.KEY_OVERVIEW + "/" + key;
        return collected.stream().filter(d -> path.equals(String.join("/", d.getName().asJavaUtilList())))
                .map(AEnvironmentData::getValue).findFirst().orElse(null);
    }

    private static String expectedMemTotal() throws IOException {
        for (String line : Files.readAllLines(MEMINFO)) {
            if (line.startsWith(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL + ":")) {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        return null;
    }

    @Test
    public void userNameIsTakenFromTheSystemProperty() throws Exception {
        measurer.contributeMeasurements(collector);

        assertEquals(System.getProperty("user.name"), valueOf("user"));
    }

    @Test
    public void jvmNameAndVersionAreCombined() throws Exception {
        measurer.contributeMeasurements(collector);

        assertEquals(System.getProperty("java.vm.name") + " " + System.getProperty("java.version"), valueOf("jvm"));
    }

    @Test
    public void osEntryContainsNameVersionAndArchitecture() throws Exception {
        measurer.contributeMeasurements(collector);

        final String os = valueOf("os");
        assertNotNull(os);
        assertTrue(os.contains(System.getProperty("os.name")), os);
        assertTrue(os.contains("(" + System.getProperty("os.arch") + ")"), os);
    }

    @Test
    public void physicalMemoryIsTakenFromMemInfo() throws Exception {
        assumeTrue(Files.isReadable(MEMINFO));

        measurer.contributeMeasurements(collector);

        assertEquals(expectedMemTotal(), valueOf("physical memory"));
    }

    @Test
    public void hostnameIsReportedIfItCanBeDetermined() throws Exception {
        measurer.contributeMeasurements(collector);

        final String hostname = valueOf("hostname");
        if (hostname != null) {
            assertFalse(hostname.isEmpty(), "hostname must not be empty if it was reported at all");
        }
    }

    @Test
    public void allEntriesAreStoredUnderTheOverviewKey() throws Exception {
        measurer.contributeMeasurements(collector);

        assertFalse(collected.isEmpty());
        assertTrue(collected.stream().allMatch(d ->
                AOverviewEnvironmentMeasurer.KEY_OVERVIEW.equals(d.getName().asJavaUtilList().get(0))));
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        measurer.contributeMeasurements(collector);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }
}
