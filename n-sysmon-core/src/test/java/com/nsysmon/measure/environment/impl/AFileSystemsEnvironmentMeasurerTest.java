package com.nsysmon.measure.environment.impl;

import com.ajjpj.afoundation.proc.CliCommand;
import com.nsysmon.NSysMon;
import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class AFileSystemsEnvironmentMeasurerTest {

    private static final Path MTAB = Path.of("/etc/mtab");
    private static final String HW = ACpuEnvironmentMeasurer.KEY_HW;
    private static final String FS = AFileSystemsEnvironmentMeasurer.KEY_FILESYSTEMS;

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;
    private AFileSystemsEnvironmentMeasurer measurer;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
        measurer = new AFileSystemsEnvironmentMeasurer();
    }

    /**
     * Unlike {@link List#copyOf(List)} this tolerates null elements: a JVM argument without ':' or '=' is stored under a
     * null key by the measurer itself.
     */
    static List<String> nameOf(AEnvironmentData data) {
        return new ArrayList<>(data.getName().asJavaUtilList());
    }

    private List<List<String>> names() {
        return collected.stream().map(AFileSystemsEnvironmentMeasurerTest::nameOf).toList();
    }

    private List<String> valuesOf(List<String> name) {
        return collected.stream().filter(d -> nameOf(d).equals(name)).map(AEnvironmentData::getValue).toList();
    }

    private static List<String> dfDeviceLines() throws Exception {
        return new CliCommand("df", "-P").getOutput().stream().filter(l -> l.startsWith("/dev/")).toList();
    }

    @Test
    public void mountPointsAreMappedFromDeviceWithoutTheDevPrefix() throws Exception {
        assumeFalse(NSysMon.isWindows());

        final Map<String, String> mountPoints = AFileSystemsEnvironmentMeasurer.getMountPoints();

        assertFalse(mountPoints.isEmpty());
        assertTrue(mountPoints.keySet().stream().noneMatch(k -> k.startsWith("/dev/")), mountPoints.keySet().toString());
        assertTrue(mountPoints.values().stream().allMatch(v -> v.startsWith("/")), mountPoints.toString());
    }

    @Test
    public void mountPointsAreTakenFromTheCorrespondingDfLine() throws Exception {
        assumeFalse(NSysMon.isWindows());
        final List<String> dfLines = dfDeviceLines();

        final Map<String, String> mountPoints = AFileSystemsEnvironmentMeasurer.getMountPoints();

        for (Map.Entry<String, String> e : mountPoints.entrySet()) {
            final String expected = dfLines.stream()
                    .filter(l -> l.split("\\s+").length == 6)
                    .filter(l -> l.startsWith("/dev/" + e.getKey()))
                    .filter(l -> l.split("\\s+")[5].equals(e.getValue()))
                    .findFirst().orElse(null);
            assertTrue(expected != null, "no df line for " + e);
        }
    }

    @Test
    public void mountPointOfEveryDeviceIsCollected() throws Exception {
        assumeFalse(NSysMon.isWindows());
        final Map<String, String> mountPoints = AFileSystemsEnvironmentMeasurer.getMountPoints();
        final String device = "/dev/" + mountPoints.keySet().iterator().next();

        measurer.contributeMeasurements(collector);

        assertTrue(valuesOf(List.of(HW, FS, device, "Mount Point")).contains(mountPoints.get(device.substring(5))), names().toString());
    }

    @Test
    public void typeAndFlagsAreTakenFromMtab() throws Exception {
        assumeTrue(Files.isReadable(MTAB));

        measurer.contributeMeasurements(collector);

        final String device = firstMtabDevice();
        final List<List<String>> fsEntries = names().stream()
                .filter(n -> n.size() == 4 && HW.equals(n.get(0)) && FS.equals(n.get(1)) && device.equals(n.get(2)))
                .toList();
        assertTrue(fsEntries.stream().anyMatch(n -> "Type".equals(n.get(3))), fsEntries.toString());
        assertTrue(fsEntries.stream().anyMatch(n -> "Flags".equals(n.get(3))), fsEntries.toString());
    }

    @Test
    public void mtabMountPointIsCollectedUnderTheDeviceKey() throws Exception {
        assumeTrue(Files.isReadable(MTAB));
        final String device = firstMtabDevice();
        final String mountPoint = Files.readAllLines(MTAB).stream()
                .map(String::trim)
                .filter(l -> l.startsWith(device + " "))
                .findFirst().orElseThrow()
                .split("\\s+")[1];

        measurer.contributeMeasurements(collector);

        final long numMtabLinesForDevice = Files.readAllLines(MTAB).stream()
                .map(String::trim).filter(l -> l.startsWith(device + " ")).count();
        assertEquals(numMtabLinesForDevice, valuesOf(List.of(HW, FS, device)).size());
        assertTrue(valuesOf(List.of(HW, FS, device)).contains(mountPoint));
    }

    @Test
    public void allJvmInputArgumentsAreReported() throws Exception {
        measurer.contributeMeasurements(collector);

        final long numJdkParams = names().stream()
                .filter(n -> AFileSystemsEnvironmentMeasurer.JVM_PARAMETERS.equals(n.get(0))).count();
        assertEquals(ManagementFactory.getRuntimeMXBean().getInputArguments().size(), numJdkParams);
    }

    @Test
    public void jvmParameterNameIsThePartBeforeTheFirstSeparator() throws Exception {
        measurer.contributeMeasurements(collector);

        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            final int idx = arg.indexOf(':') != -1 ? arg.indexOf(':') : arg.indexOf('=');
            final List<String> expectedName = idx != -1 ? List.of(AFileSystemsEnvironmentMeasurer.JVM_PARAMETERS,
                    arg.substring(0, idx)) : Arrays.asList(AFileSystemsEnvironmentMeasurer.JVM_PARAMETERS, null);
            assertFalse(valuesOf(expectedName).isEmpty(), "expected an entry for jvm parameter '" + arg + "'");
        }
    }

    @Test
    public void everyEntryIsStoredBelowTheHardwareOrJvmParameterKey() throws Exception {
        measurer.contributeMeasurements(collector);

        assertFalse(collected.isEmpty());
        assertTrue(names().stream().allMatch(n -> ACpuEnvironmentMeasurer.KEY_HW.equals(n.get(0))
                || AFileSystemsEnvironmentMeasurer.JVM_PARAMETERS.equals(n.get(0))), names().toString());
    }

    @Test
    public void noFileSystemIsCollectedOnWindows() throws Exception {
        final String osName = System.getProperty("os.name");
        System.setProperty("os.name", "Windows 11");
        try {
            measurer.contributeMeasurements(collector);
        } finally {
            System.setProperty("os.name", osName);
        }

        assertTrue(names().stream().noneMatch(n -> n.get(0).equals(FS) || n.get(0).equals(HW)), names().toString());
        assertTrue(names().stream().allMatch(n -> n.get(0).equals(AFileSystemsEnvironmentMeasurer.JVM_PARAMETERS)),
                names().toString());
    }

    @Test
    public void shutdownLeavesAlreadyCollectedDataUntouched() throws Exception {
        measurer.contributeMeasurements(collector);
        final int numCollected = collected.size();

        measurer.shutdown();

        assertEquals(numCollected, collected.size());
    }

    private static String firstMtabDevice() throws IOException {
        return Files.readAllLines(MTAB).stream()
                .map(String::trim)
                .filter(l -> l.startsWith("/dev/") && l.split(" ").length >= 4)
                .map(l -> l.split(" ")[0])
                .findFirst().orElseThrow();
    }
}