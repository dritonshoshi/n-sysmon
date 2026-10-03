package com.nsysmon.servlet.trace;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.measure.http.ASimpleHttpRequestAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ATraceFilterTest {

    private static AHierarchicalDataRoot traceWithRootParams(Map<String, String> params) {
        return new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 1L, "root", params, Collections.emptyList(), false),
                Collections.emptyList(), Collections.emptyList());
    }

    @Test void allFilterCollectsEverything() {
        assertTrue(ATraceFilter.ALL.shouldCollect(traceWithRootParams(Map.of())));
        assertTrue(ATraceFilter.ALL.shouldCollect(traceWithRootParams(Map.of("anything", "at all"))));
    }

    @Test void allFilterIsLabelledForAllTraces() {
        assertEquals("tracesAll", ATraceFilter.ALL.getId());
        assertEquals("All Traces", ATraceFilter.ALL.getShortLabel());
        assertEquals("All Traces", ATraceFilter.ALL.getFullLabel());
    }

    @Test void httpFilterCollectsTracesWithAFullUrl() {
        assertTrue(ATraceFilter.HTTP.shouldCollect(
                traceWithRootParams(Map.of(ASimpleHttpRequestAnalyzer.PARAM_FULL_URL, "http://localhost/a"))));
    }

    @Test void httpFilterIgnoresTracesWithoutAFullUrl() {
        assertFalse(ATraceFilter.HTTP.shouldCollect(traceWithRootParams(Map.of("something-else", "x"))));
        assertFalse(ATraceFilter.HTTP.shouldCollect(traceWithRootParams(Map.of())));
    }

    @Test void httpFilterLooksAtTheRootNodeOnly() {
        final AHierarchicalData child = new AHierarchicalData(true, 1L, 1L, "child",
                Map.of(ASimpleHttpRequestAnalyzer.PARAM_FULL_URL, "http://localhost/a"), List.of(), false);
        final AHierarchicalDataRoot root = new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, 2L, "root", Map.of(), List.of(child), false),
                Collections.emptyList(), Collections.emptyList());

        assertFalse(ATraceFilter.HTTP.shouldCollect(root));
    }

    @Test void httpFilterIsLabelledForHttpRequests() {
        assertEquals("tracesHttp", ATraceFilter.HTTP.getId());
        assertEquals("HTTP Requests", ATraceFilter.HTTP.getShortLabel());
        assertEquals("Traces for all HTTP requests", ATraceFilter.HTTP.getFullLabel());
    }
}
