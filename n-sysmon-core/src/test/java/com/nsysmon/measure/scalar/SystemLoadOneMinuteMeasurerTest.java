package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class SystemLoadOneMinuteMeasurerTest {

    private static final String IDENT_LOAD = "Load";

    @TempDir
    private Path tempDir;

    private SystemLoadOneMinuteMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() throws Exception {
        measurer = new SystemLoadOneMinuteMeasurer();
        data = new HashMap<>();
        useLoadAvgFile("1.50 2.25 3.00 1/100 1234\n");
    }

    private void useLoadAvgFile(String content) throws Exception {
        final File file = tempDir.resolve("loadavg").toFile();
        Files.writeString(file.toPath(), content);
        final Field procFile = SystemLoadOneMinuteMeasurer.class.getDeclaredField("procFile");
        procFile.setAccessible(true);
        procFile.set(measurer, file);
    }

    @Test
    public void onlyTheOneMinuteAverageIsReported() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(1, data.size());
        assertEquals(150, data.get(IDENT_LOAD).getValueRaw());
    }

    @Test
    public void theAverageIsReportedWithTwoFractionDigits() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(2, data.get(IDENT_LOAD).getNumFracDigits());
        assertEquals(1000L, data.get(IDENT_LOAD).getTimestamp());
    }

    @Test
    public void laterAveragesAreIgnored() throws Exception {
        useLoadAvgFile("5.00 1.00 1.00 1/100 1234\n");

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(500, data.get(IDENT_LOAD).getValueRaw());
    }

    @Test
    public void laterAveragesAreNotRequiredAtAll() throws Exception {
        useLoadAvgFile("1.00\n");

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(100, data.get(IDENT_LOAD).getValueRaw());
    }

    @Test
    public void anEmptyFileIsReportedAsNullPointerException() throws Exception {
        useLoadAvgFile("");

        assertThrows(NullPointerException.class,
                () -> measurer.contributeMeasurements(data, 1000L, new HashMap<>()));
    }

    @Test
    public void theLoadBelongsToTheSystemGroup() {
        assertEquals("System", measurer.getGroupnameOfMeasurement(IDENT_LOAD));
        assertEquals("System", measurer.getGroupnameOfMeasurement("load"));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test
    public void theLoadIsDescribed() {
        assertTrue(measurer.getDescriptionOfMeasurement(IDENT_LOAD).contains("system load"));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
        assertTrue(measurer.isResponsibleForMeasurement(IDENT_LOAD));
    }

    @Test
    public void prepareMeasurementsAndShutdownAreNoops() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);
        measurer.shutdown();

        assertTrue(mementos.isEmpty());
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}