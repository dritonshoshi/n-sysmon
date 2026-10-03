package com.nsysmon.measure.environment.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class AMemInfoEnvironmentMeasurerTest {

    private static final Path MEMINFO = Path.of("/proc/meminfo");

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AMemInfoEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AMemInfoEnvironmentMeasurer();
    }

    private static Map<String, String> expectedMemInfo() throws IOException {
        final Map<String, String> result = new HashMap<>();
        for (String line : Files.readAllLines(MEMINFO)) {
            final int idx = line.indexOf(':');
            if (idx < 0) {
                continue;
            }
            result.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
        }
        return result;
    }

    private List<String> valuesNamed(String name) {
        final String path = ACpuEnvironmentMeasurer.KEY_HW + "/" + AMemInfoEnvironmentMeasurer.KEY_MEMINFO + "/" + name;
        return collected.stream().filter(d -> path.equals(String.join("/", d.getName().asJavaUtilList())))
                .map(AEnvironmentData::getValue).toList();
    }

    @Test
    public void memTotalIsParsedWithItsUnit() throws Exception {
        assumeTrue(Files.isReadable(MEMINFO));

        final Map<String, String> memInfo = measurer.read();

        assertEquals(expectedMemInfo().get(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL),
                memInfo.get(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL));
        assertTrue(memInfo.get(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL).endsWith(" kB"));
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        measurer.contributeMeasurements(collector);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }

    @Test
    public void keysAreTakenFromThePartBeforeTheColon() throws Exception {
        assumeTrue(Files.isReadable(MEMINFO));

        final Map<String, String> memInfo = measurer.read();

        assertFalse(memInfo.isEmpty());
        assertTrue(memInfo.keySet().stream().noneMatch(k -> k.contains(":")), memInfo.keySet().toString());
        assertTrue(memInfo.keySet().contains("MemFree"));
    }

    @Test
    public void everyEntryOfMemInfoIsCollected() throws Exception {
        assumeTrue(Files.isReadable(MEMINFO));

        measurer.contributeMeasurements(collector);

        assertEquals(expectedMemInfo().size(), collected.size());
        assertEquals(expectedMemInfo().get(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL),
                valuesNamed(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL).get(0));
    }

    @Test
    public void nothingIsReadOnWindows() throws Exception {
        final String osName = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 11");
        try {
            assertTrue(measurer.read().isEmpty());
        } finally {
            System.setProperty("os.name", osName);
        }
    }

    @Test
    public void valuesKeepTheirUnits() throws Exception {
        assumeTrue(Files.isReadable(MEMINFO));

        measurer.contributeMeasurements(collector);

        assertTrue(valuesNamed(AMemInfoEnvironmentMeasurer.KEY_DETAIL_MEM_TOTAL).get(0).matches("[0-9]+ kB"));
    }
}
