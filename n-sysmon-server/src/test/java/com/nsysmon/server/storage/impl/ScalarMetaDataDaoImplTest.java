package com.nsysmon.server.storage.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.nsysmon.server.storage.ScalarMetaData;
import com.nsysmon.server.util.AOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScalarMetaDataDaoImplTest {
    @Mock private DB db;
    @Mock private DBCollection coll;

    private ScalarMetaDataDaoImpl underTest;

    @BeforeEach void createDao() {
        when(db.getCollection(ScalarMetaDataDaoImpl.COLL_SCALAR_META_DATA)).thenReturn(coll);
        underTest = new ScalarMetaDataDaoImpl(db);
    }

    @Test void readsTheScalarMetaDataFromTheMetaDataCollection() {
        verify(db).getCollection("environment-metadata");
    }

    @Test void getAllKeysTheMetaDataByScalarName() {
        givenDocuments(
                new BasicDBObject("_id", "cpu.load").append("numFracDigits", 3),
                new BasicDBObject("_id", "heap.used").append("numFracDigits", 0));

        final Map<String, ScalarMetaData> result = underTest.getAll();

        assertEquals(2, result.size());
        assertEquals(3, result.get("cpu.load").numFracDigits);
        assertEquals(0, result.get("heap.used").numFracDigits);
    }

    @Test void getAllDefaultsTheNumberOfFractionDigitsToOne() {
        givenDocuments(new BasicDBObject("_id", "cpu.load"));

        assertEquals(1, underTest.getAll().get("cpu.load").numFracDigits);
    }

    @Test void getAllIsEmptyWhenNoMetaDataIsStored() {
        givenDocuments();

        assertTrue(underTest.getAll().isEmpty());
    }

    @Test void getReturnsTheMetaDataOfAKnownScalar() {
        when(coll.findOne(new BasicDBObject("_id", "cpu.load")))
                .thenReturn(new BasicDBObject("_id", "cpu.load").append("numFracDigits", 4));

        final AOption<ScalarMetaData> result = underTest.get("cpu.load");

        assertTrue(result.isDefined());
        assertEquals("cpu.load", result.get().name);
        assertEquals(4, result.get().numFracDigits);
    }

    @Test void getIsEmptyForAnUnknownScalar() {
        when(coll.findOne(any(DBObject.class))).thenReturn(null);

        assertTrue(underTest.get("unknown").isEmpty());
    }

    @Test void getQueriesTheDocumentById() {
        when(coll.findOne(any(DBObject.class))).thenReturn(null);

        underTest.get("cpu.load");

        verify(coll).findOne(new BasicDBObject("_id", "cpu.load"));
    }

    @Test void storeUpsertsTheDocumentKeyedByScalarName() {
        underTest.store(new ScalarMetaData("cpu.load", 2));

        verify(coll).update(new BasicDBObject("_id", "cpu.load"),
                new BasicDBObject("_id", "cpu.load").append("numFracDigits", 2), true, false);
    }

    private void givenDocuments(DBObject... documents) {
        final DBCursor cursor = mock(DBCursor.class);
        when(cursor.iterator()).thenReturn(Arrays.asList(documents).iterator());
        when(coll.find()).thenReturn(cursor);
    }
}
