package com.nsysmon.server.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListWrapperTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void exposesWrappedList() {
        final List<String> data = List.of("a", "b");

        assertEquals(data, new ListWrapper<>(data).getData());
    }

    @Test void toStringMentionsWrappedData() {
        assertEquals("ListWrapper{data=[a, b]}", new ListWrapper<>(List.of("a", "b")).toString());
    }

    @Test void serializesAsJsonObjectWithDataProperty() throws Exception {
        assertEquals("{\"data\":[\"a\",\"b\"]}",
                om.writeValueAsString(new ListWrapper<>(Arrays.asList("a", "b"))));
    }

    @Test void serializesAnEmptyListAsEmptyArray() throws Exception {
        assertEquals("{\"data\":[]}", om.writeValueAsString(new ListWrapper<>(new ArrayList<>())));
    }

    @Test void cannotBeDeserializedBecauseItHasNoDefaultConstructor() {
        assertThrows(JsonProcessingException.class, () -> om.readValue("{\"data\":[\"a\"]}", ListWrapper.class));
    }

    @Test void wrapsMutableListWithoutCopyingIt() {
        final List<String> data = new ArrayList<>();
        final ListWrapper<String> underTest = new ListWrapper<>(data);

        data.add("a");

        assertTrue(underTest.getData().contains("a"));
    }
}
