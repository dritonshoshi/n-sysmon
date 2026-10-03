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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class ACpuEnvironmentMeasurerTest {

    private static final Path CPUINFO = Path.of("/proc/cpuinfo");
    private static final String CPU_PATH = ACpuEnvironmentMeasurer.KEY_HW + "/" + ACpuEnvironmentMeasurer.KEY_CPUS;

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private ACpuEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new ACpuEnvironmentMeasurer();
    }

    private static List<String> cpuinfoLines() throws IOException {
        return Files.readAllLines(CPUINFO);
    }

    private static long expectedNumCpus() throws IOException {
        return cpuinfoLines().stream().filter(l -> l.startsWith("processor")).count();
    }

    private List<String> namesUnder() {
        return collected.stream().map(d -> String.join("/", d.getName().asJavaUtilList())).toList();
    }

    private List<String> valuesOf(String name) {
        return collected.stream()
                .filter(d -> name.equals(String.join("/", d.getName().asJavaUtilList())))
                .map(AEnvironmentData::getValue)
                .toList();
    }

    @Test
    public void numberOfCpusIsReportedAsItsOwnEntry() throws Exception {
        assumeTrue(Files.isReadable(CPUINFO));

        measurer.contributeMeasurements(collector);

        assertEquals(List.of(String.valueOf(expectedNumCpus())), valuesOf(CPU_PATH));
    }

    @Test
    public void modelNamesAreReportedWithoutTheRawKey() throws Exception {
        assumeTrue(Files.isReadable(CPUINFO));

        measurer.contributeMeasurements(collector);

        final List<String> modelNames = valuesOf(CPU_PATH + "/cpu 0");
        assertFalse(modelNames.isEmpty());
        assertTrue(modelNames.stream().allMatch(v -> !v.isEmpty()), modelNames.toString());
        assertTrue(namesUnder().stream().noneMatch(n -> n.startsWith(CPU_PATH + "/model name")), namesUnder().toString());
    }

    @Test
    public void furtherCpuInfoIsReportedWithTheKeyAsLastSegment() throws Exception {
        assumeTrue(Files.isReadable(CPUINFO));
        assumeTrue(cpuinfoLines().stream().anyMatch(l -> l.startsWith("cpu MHz")), "no cpu MHz in /proc/cpuinfo");

        measurer.contributeMeasurements(collector);

        final List<String> mhzValues = valuesOf(CPU_PATH + "/cpu 0/cpu MHz");
        assertFalse(mhzValues.isEmpty(), namesUnder().toString());
        assertTrue(mhzValues.stream().allMatch(v -> v.matches("[0-9]+(\\.[0-9]+)?")), mhzValues.toString());
    }

    @Test
    public void allEntriesAreStoredBelowTheCpuKey() throws Exception {
        assumeTrue(Files.isReadable(CPUINFO));

        measurer.contributeMeasurements(collector);

        assertFalse(collected.isEmpty());
        assertTrue(collected.stream().allMatch(d ->
                ACpuEnvironmentMeasurer.KEY_HW.equals(d.getName().asJavaUtilList().get(0))), namesUnder().toString());
    }

    @Test
    public void nothingIsCollectedOnWindows() throws Exception {
        final String osName = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 11");
        try {
            measurer.contributeMeasurements(collector);
        } finally {
            System.setProperty("os.name", osName);
        }

        assertTrue(collected.isEmpty(), collected.size() + " entries were collected");
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        assumeTrue(Files.isReadable(CPUINFO));
        measurer.contributeMeasurements(collector);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }
}