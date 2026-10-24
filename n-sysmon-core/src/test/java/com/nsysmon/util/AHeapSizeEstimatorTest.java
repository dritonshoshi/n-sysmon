package com.nsysmon.util;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AHeapSizeEstimatorTest {

    @Test void nullHasNoSize() {
        assertEquals(0, new AHeapSizeEstimator().estimate(null));
    }

    @Test void countsStrings() {
        assertTrue(new AHeapSizeEstimator().estimate("some SQL statement that is retained") > 20);
    }

    @Test void countsTheFullCapacityOfAnObjectArray() {
        assertTrue(new AHeapSizeEstimator().estimate(new Object[1000]) >= 4000);
    }

    @Test void countsPrimitiveArrays() {
        assertTrue(new AHeapSizeEstimator().estimate(new long[100]) >= 800);
    }

    @Test void countsElementsOfCollections() {
        assertTrue(new AHeapSizeEstimator().estimate(List.of("a", "b", "c")) > new AHeapSizeEstimator().estimate(List.of("a")));
    }

    @Test void countsMapKeysAndValues() {
        final Map<String, String> map = new HashMap<>();
        map.put("jdbc: select * from a_very_long_table_name where x = ?", "value");

        assertTrue(new AHeapSizeEstimator().estimate(map) > 60);
    }

    @Test void handlesCycles() {
        final List<Object> list = new ArrayList<>();
        list.add(list);

        assertTrue(new AHeapSizeEstimator().estimate(list) > 0);
    }

    @Test void countsSharedObjectsOnlyOnce() {
        final Object shared = new byte[10_000];
        final long once = new AHeapSizeEstimator().estimate(List.of(shared));
        final long twice = new AHeapSizeEstimator().estimate(List.of(shared, shared));

        assertTrue(twice - once < 100, "shared object must not be counted twice but was: " + once + " -> " + twice);
    }

    @Test void countsThePreallocatedRingBufferArray() {
        final ARingBuffer<String> buffer = new ARingBuffer<>(String.class, 1000);

        assertTrue(new AHeapSizeEstimator().estimate(buffer) >= 4000,
                "the preallocated buffer array has to be part of the estimate");
    }

    @Test void countsTheElementsOfARingBuffer() {
        final ARingBuffer<String> buffer = new ARingBuffer<>(String.class, 10);
        buffer.put("a value that is retained by the buffer");

        assertTrue(new AHeapSizeEstimator().estimate(buffer) > new AHeapSizeEstimator().estimate(new ARingBuffer<>(String.class, 10)));
    }

    @Test void countReturnsTheNumberOfElements() {
        assertEquals(3, AHeapSizeEstimator.count(List.of("a", "b", "c")));
    }

    @Test void countReturnsMinusOneForNull() {
        assertEquals(-1, AHeapSizeEstimator.count(null));
    }
}
