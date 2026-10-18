package com.nsysmon.servlet.memory;

import com.nsysmon.datasink.MemoryUsageKind;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.testutil.MemoryReportingDataSink;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSysMonMemoryPageDefinitionTest {
    private NSysMonImpl sysMon;
    private RecordingJson json;

    @BeforeEach void setUp() {
        sysMon = (NSysMonImpl) TestSysMons.create();
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        sysMon.shutdown();
    }

    @Test void exposesItsLabelsAndFileNames() {
        final NSysMonMemoryPageDefinition page = new NSysMonMemoryPageDefinition();

        assertEquals("nsysmonMemory", page.getId());
        assertEquals("NSysMon Memory", page.getShortLabel());
        assertEquals("NSysMon Memory Usage", page.getFullLabel());
        assertEquals("memoryusage.html", page.getHtmlFileName());
        assertEquals("CtrlMemoryUsage", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        final NSysMonMemoryPageDefinition page = new NSysMonMemoryPageDefinition();
        page.init(sysMon);

        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesTheDataSinkReport() throws Exception {
        final MemoryReportingDataSink sink = new MemoryReportingDataSink("my sink", MemoryUsageKind.RAW_HIERARCHY);
        NSysMonConfigurer.addDataSink(sysMon, sink);

        final String result = servedData();

        assertTrue(result.contains("\"name\":\"my sink\""), result);
        assertTrue(result.contains("\"kind\":\"RAW_HIERARCHY\""), result);
        assertTrue(result.contains("\"reporting\":true"), result);
        assertTrue(result.contains("\"dataSinks\":["), result);
        assertTrue(result.contains("\"timedScalars\":["), result);
    }

    @Test void servesTheJvmHeapValues() throws Exception {
        final String result = servedData();

        assertTrue(result.contains("\"jvmUsedHeapBytes\":"), result);
        assertTrue(result.contains("\"jvmMaxHeapBytes\":"), result);
    }

    private String servedData() throws Exception {
        final NSysMonMemoryPageDefinition page = new NSysMonMemoryPageDefinition();
        page.init(sysMon);

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));
        return json.compactJson();
    }
}
