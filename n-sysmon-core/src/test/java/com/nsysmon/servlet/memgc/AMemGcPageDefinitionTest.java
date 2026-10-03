package com.nsysmon.servlet.memgc;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AMemGcPageDefinitionTest {
    private NSysMonApi sysMon;
    private AMemGcPageDefinition page;
    private RecordingJson json;

    @BeforeEach void setUp() {
        sysMon = TestSysMons.create();
        page = new AMemGcPageDefinition(10);
        page.init(sysMon);
        json = new RecordingJson();
    }

    private static Map<String, String> gcParams() {
        final Map<String, String> params = new HashMap<>();
        params.put(AJmxGcMeasurer.KEY_CAUSE, "G1 Evacuation Pause");
        params.put(AJmxGcMeasurer.KEY_ALGORITHM, "G1");
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + "heap" + AJmxGcMeasurer.KEY_SUFFIX_USED, "1000");
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + "heap" + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED, "4000");
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + "heap" + AJmxGcMeasurer.KEY_SUFFIX_USED_DELTA, "100");
        params.put(AJmxGcMeasurer.KEY_PREFIX_MEM + "heap" + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED_DELTA, "250");
        return params;
    }

    private void givenGarbageCollection() {
        final AHierarchicalDataRoot root = new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1_000L, 99L, AJmxGcMeasurer.IDENT_GC_TRACE_ROOT, gcParams(),
                        List.of(new AHierarchicalData(true, 1_000L, 5L, "G1 New", Map.of(), List.of(), false)),
                        false),
                Collections.emptyList(), Collections.emptyList());
        sysMon.injectSyntheticMeasurement(root);
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("memgc", page.getId());
        assertEquals("GC View", page.getShortLabel());
        assertEquals("Garbage Collection History", page.getFullLabel());
        assertEquals("memgc.html", page.getHtmlFileName());
        assertEquals("CtrlMemGc", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyListBeforeAnyGcHappened() throws Exception {
        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));

        assertEquals("{\"gcs\":[]}", json.compactJson());
    }

    @Test void servesTheCollectedGarbageCollections() throws Exception {
        givenGarbageCollection();

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));

        assertEquals("{\"gcs\":[{\"type\":\"G1 New\",\"algorithm\":\"G1\",\"cause\":\"G1 Evacuation Pause\","
                        + "\"startMillis\":1000,\"durationNanos\":5,\"mem\":{\"heap\":{"
                        + "\"usedBefore\":900,\"usedAfter\":1000,"
                        + "\"committedBefore\":3750,\"committedAfter\":4000}}}]}",
                json.compactJson());
    }

    @Test void exportsTheSameDataAsTheRestCall() throws Exception {
        givenGarbageCollection();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        page.getDataForExport(out);

        assertEquals("{\"gcs\":[{\"type\":\"G1 New\",\"algorithm\":\"G1\",\"cause\":\"G1 Evacuation Pause\","
                        + "\"startMillis\":1000,\"durationNanos\":5,\"mem\":{\"heap\":{"
                        + "\"usedBefore\":900,\"usedAfter\":1000,"
                        + "\"committedBefore\":3750,\"committedAfter\":4000}}}]}",
                out.toString(StandardCharsets.UTF_8).replace("\n", ""));
    }
}
