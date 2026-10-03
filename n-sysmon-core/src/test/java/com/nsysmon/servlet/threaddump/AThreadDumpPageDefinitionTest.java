package com.nsysmon.servlet.threaddump;

import com.nsysmon.NSysMonApi;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.MonitorDeadlock;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AThreadDumpPageDefinitionTest {
    private NSysMonApi sysMon;
    private RecordingJson json;

    @BeforeEach void setUp() {
        sysMon = TestSysMons.create();
        json = new RecordingJson();
    }

    @Test void exposesItsLabelsAndFileNames() {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");

        assertEquals("threaddump", page.getId());
        assertEquals("Thread Dump", page.getShortLabel());
        assertEquals("Thread Dump", page.getFullLabel());
        assertEquals("threaddump.html", page.getHtmlFileName());
        assertEquals("CtrlThreadDump", page.getControllerName());
    }

    @Test void appendsIdAndLabelSuffixes() {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example", "2", " (2)");

        assertEquals("threaddump2", page.getId());
        assertEquals("Thread Dump (2)", page.getShortLabel());
        assertEquals("Thread Dump (2)", page.getFullLabel());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");
        page.init(sysMon);

        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void writesTheApplicationPackageAndAllThreads() throws Exception {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");
        page.init(sysMon);

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));

        final String result = json.compactJson();
        assertTrue(result.startsWith("{\"appPkg\":\"com.example\",\"threads\":[{"), result);
        assertTrue(result.contains("\"name\":\"" + Thread.currentThread().getName() + "\""), result);
    }

    @Test void writesIdStateAndStackTraceForEveryThread() throws Exception {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");
        page.init(sysMon);
        page.handleRestCall("getData", List.of(), json.helper());

        final String result = json.compactJson();
        assertTrue(result.contains("\"id\":" + Thread.currentThread().getId() + ","), "missing thread id");
        assertTrue(result.contains("\"state\":\""), "missing state");
        assertTrue(result.contains("\"stacktrace\":[{"), "missing stacktrace");
        assertTrue(result.contains("\"hasSource\":"), "missing hasSource");
        assertTrue(result.contains("\"isNative\":"), "missing isNative");
        assertTrue(result.contains("\"repr\":\""), "missing repr");
    }

    @Test void writesRunningMillisForThreadsWithATrackedMeasurement() throws Exception {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");
        page.init(sysMon);
        final ASimpleMeasurement measurement = sysMon.start("stillRunning");

        page.handleRestCall("getData", List.of(), json.helper());
        measurement.finish();

        final String entry = json.compactJson();
        assertTrue(entry.contains("\"runningMillis\":"), entry);
    }

    @Test void marksDeadlockedThreads() throws Exception {
        final AThreadDumpPageDefinition page = new AThreadDumpPageDefinition("com.example");
        page.init(sysMon);
        MonitorDeadlock.create("dump-deadlock-a", "dump-deadlock-b");

        page.handleRestCall("getData", List.of(), json.helper());

        assertTrue(json.compactJson().contains("\"state\":\"DEADLOCKED\""), "no deadlocked thread reported");
    }
}
