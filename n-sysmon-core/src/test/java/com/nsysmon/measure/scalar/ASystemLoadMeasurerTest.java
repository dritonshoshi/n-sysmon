package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class ASystemLoadMeasurerTest {

    @TempDir
    private Path tempDir;

    private ASystemLoadMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() throws Exception {
        measurer = new ASystemLoadMeasurer();
        data = new HashMap<>();
        useLoadAvgFile("1.50 2.25 3.00 1/100 1234\n");
    }

    private void useLoadAvgFile(String content) throws Exception {
        final File file = tempDir.resolve("loadavg").toFile();
        Files.writeString(file.toPath(), content);
        final Field procFile = ASystemLoadMeasurer.class.getDeclaredField("procFile");
        procFile.setAccessible(true);
        procFile.set(measurer, file);
    }

    @Test
    public void allThreeAveragesAreReportedScaledByHundred() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(3, data.size());
        assertEquals(150, data.get(ASystemLoadMeasurer.IDENT_LOAD_1_MIN).getValueRaw());
        assertEquals(225, data.get(ASystemLoadMeasurer.IDENT_LOAD_5_MIN).getValueRaw());
        assertEquals(300, data.get(ASystemLoadMeasurer.IDENT_LOAD_15_MIN).getValueRaw());
    }

    @Test
    public void allAveragesAreReportedWithTwoFractionDigits() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getNumFracDigits() == 2));
    }

    @Test
    public void timestampIsPassedThroughToEveryDataPoint() throws Exception {
        measurer.contributeMeasurements(data, 4711L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 4711L));
    }

    @Test
    public void roundingTruncatesInsteadOfRoundingUp() throws Exception {
        useLoadAvgFile("0.29 0.29 0.29 1/100 1234\n");

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(28, data.get(ASystemLoadMeasurer.IDENT_LOAD_1_MIN).getValueRaw());
    }

    @Test
    public void aLoadOfZeroIsReportedAsZero() throws Exception {
        useLoadAvgFile("0.00 0.00 0.00 1/100 1234\n");

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(0, data.get(ASystemLoadMeasurer.IDENT_LOAD_1_MIN).getValueRaw());
    }

    @Test
    public void onlyTheFirstLineIsEvaluated() throws Exception {
        useLoadAvgFile("1.00 1.00 1.00 1/100 1\n9.00 9.00 9.00 1/100 2\n");

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(100, data.get(ASystemLoadMeasurer.IDENT_LOAD_1_MIN).getValueRaw());
    }

    @Test
    public void incompleteContentIsRejected() throws Exception {
        useLoadAvgFile("1.00 2.00\n");

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> measurer.contributeMeasurements(data, 1000L, new HashMap<>()));
    }

    @Test
    public void nonNumericContentIsRejected() throws Exception {
        useLoadAvgFile("not-a-number 2.00 3.00 1/100 1234\n");

        assertThrows(NumberFormatException.class,
                () -> measurer.contributeMeasurements(data, 1000L, new HashMap<>()));
    }

    @Test
    public void missingFileIsReportedAsIoException() throws Exception {
        final Field procFile = ASystemLoadMeasurer.class.getDeclaredField("procFile");
        procFile.setAccessible(true);
        procFile.set(measurer, tempDir.resolve("does-not-exist").toFile());

        assertThrows(IOException.class, () -> measurer.contributeMeasurements(data, 1000L, new HashMap<>()));
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
