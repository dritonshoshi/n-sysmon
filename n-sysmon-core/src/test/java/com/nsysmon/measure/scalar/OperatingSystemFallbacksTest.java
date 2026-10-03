package com.nsysmon.measure.scalar;

import com.nsysmon.NSysMon;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The measurers detect the operating system via {@link NSysMon#isWindows()} and {@link NSysMon#isMacOS()}, which are
 * plain system property checks. Faking that property is the only way to reach the platform specific branches of these
 * measurers when running on Linux.
 *
 * @author arno
 */
public class OperatingSystemFallbacksTest {

    private String originalOsName;

    @BeforeEach
    public void rememberOsName() {
        originalOsName = System.getProperty("os.name");
    }

    @AfterEach
    public void restoreOsName() {
        System.setProperty("os.name", originalOsName);
    }

    private void pretendToBe(String osName) {
        System.setProperty("os.name", osName);
    }

    private static List<AScalarMeasurer> procSnapshotBasedMeasurers() {
        return List.of(new ACpuUtilizationMeasurer(), new AProcDiskstatsMeasurer(),
                new AProcNetDevMeasurer(), new AProcSelfStatMeasurer());
    }

    @Test
    public void theFakedWindowsOsNameIsDetected() {
        pretendToBe("Windows 11");

        assertTrue(NSysMon.isWindows());
        assertFalse(NSysMon.isMacOS());
    }

    @Test
    public void theFakedMacOsNameIsDetected() {
        pretendToBe("Mac OS X");

        assertTrue(NSysMon.isMacOS());
        assertFalse(NSysMon.isWindows());
    }

    @Test
    public void noProcSnapshotIsPreparedOnWindows() throws Exception {
        pretendToBe("Windows 11");

        for (AScalarMeasurer measurer : procSnapshotBasedMeasurers()) {
            final Map<String, Object> mementos = new HashMap<>();

            measurer.prepareMeasurements(mementos);

            assertTrue(mementos.isEmpty(), measurer.getClass().getSimpleName() + " stored " + mementos.keySet());
        }
    }

    @Test
    public void noProcSnapshotIsPreparedOnMacOs() throws Exception {
        pretendToBe("Mac OS X");

        for (AScalarMeasurer measurer : procSnapshotBasedMeasurers()) {
            final Map<String, Object> mementos = new HashMap<>();

            measurer.prepareMeasurements(mementos);

            assertTrue(mementos.isEmpty(), measurer.getClass().getSimpleName() + " stored " + mementos.keySet());
        }
    }

    @Test
    public void noProcBasedValueIsContributedOnWindows() throws Exception {
        pretendToBe("Windows 11");

        assertOnlyCpuCapacityIsContributed(new ASystemLoadMeasurer());
        assertOnlyCpuCapacityIsContributed(new SystemLoadOneMinuteMeasurer());
        assertOnlyCpuCapacityIsContributed(new AProcNetDevMeasurer());
        assertOnlyCpuCapacityIsContributed(new AProcSelfStatMeasurer());
    }

    @Test
    public void noLoadValueIsContributedOnMacOs() throws Exception {
        pretendToBe("Mac OS X");

        for (AScalarMeasurer measurer : List.of(new ASystemLoadMeasurer(), new SystemLoadOneMinuteMeasurer(),
                new AProcNetDevMeasurer(), new AProcSelfStatMeasurer())) {
            final Map<String, AScalarDataPoint> data = new HashMap<>();

            measurer.contributeMeasurements(data, 1000L, new HashMap<>());

            assertTrue(data.isEmpty(), measurer.getClass().getSimpleName() + " reported " + data.keySet());
        }
    }

    private static void assertOnlyCpuCapacityIsContributed(AScalarMeasurer measurer) throws Exception {
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.isEmpty(), measurer.getClass().getSimpleName() + " reported " + data.keySet());
    }

    @Test
    public void cpuCapacityIsTakenFromTheOperatingSystemBeanOnWindows() throws Exception {
        pretendToBe("Windows 11");
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        new ACpuUtilizationMeasurer().contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 1000L,
                data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw());
        assertTrue(data.get(ACpuUtilizationMeasurer.KEY_SELF_KERNEL).getValueRaw() >= 0);
    }

    @Test
    public void cpuCapacityIsTakenFromTheOperatingSystemBeanOnMacOs() throws Exception {
        pretendToBe("Mac OS X");
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        new ACpuUtilizationMeasurer().contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 1000L,
                data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw());
        assertTrue(data.get(ACpuUtilizationMeasurer.KEY_SELF_KERNEL).getValueRaw() >= 0);
    }

    @Test
    public void noCpuFrequencyIsReportedOutsideOfLinux() throws Exception {
        pretendToBe("Windows 11");
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        new ACpuUtilizationMeasurer().contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.keySet().stream().noneMatch(k -> k.startsWith(ACpuUtilizationMeasurer.KEY_PREFIX_MHZ)),
                data.toString());
    }

    @Test
    public void diskSizesAreTakenFromTheFileSystemRootsOutsideOfLinux() throws Exception {
        for (String osName : List.of("Windows 11", "Mac OS X")) {
            pretendToBe(osName);
            final Map<String, AScalarDataPoint> data = new HashMap<>();

            new AProcDiskstatsMeasurer().contributeMeasurements(data, 1000L, new HashMap<>());

            for (File root : File.listRoots()) {
                final String dev = root.getAbsolutePath().replace(":", "").replace("\\", "");
                for (String suffix : List.of(":sizeGB", ":usedGB", ":availableGB")) {
                    assertTrue(data.containsKey("disk:" + dev + suffix), osName + ": disk:" + dev + suffix + " missing");
                }
                assertTrue(data.get("disk:" + dev + ":sizeGB").getValueRaw() >= 0);
            }
            assertTrue(data.keySet().stream().noneMatch(k -> k.contains(":mountpoint:")), osName + ": " + data.keySet());
            assertTrue(data.keySet().stream().noneMatch(k -> k.endsWith(":read-sectors")), osName + ": " + data.keySet());
        }
    }
}