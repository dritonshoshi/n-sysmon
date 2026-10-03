package com.nsysmon.server.storage.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoredApplicationDaoImplTest {
    @Mock private DB db;
    @Mock private DBCollection collection;
    @Mock private DBCursor cursor;

    private MonitoredApplicationDaoImpl underTest;

    @BeforeEach void createDao() {
        underTest = new MonitoredApplicationDaoImpl(db);
    }

    @Test void readsApplicationNamesFromTheApplicationMetaDataCollection() {
        givenDocuments(new BasicDBObject("_id", "app-1"), new BasicDBObject("_id", "app-2"));

        assertEquals(List.of("app-1", "app-2"), underTest.getMonitoredApplicationNames());
    }

    @Test void usesTheApplicationMetaDataCollection() {
        givenDocuments();

        underTest.getMonitoredApplicationNames();

        verify(db).getCollection(MonitoredApplicationDaoImpl.COLL_APPLICATION_META_DATA);
    }

    @Test void returnsAnEmptyListWhenNoApplicationIsMonitored() {
        givenDocuments();

        assertTrue(underTest.getMonitoredApplicationNames().isEmpty());
    }

    @Test void readsRawApplicationDocuments() {
        final DBObject document = new BasicDBObject("_id", "app-1").append("extra", "value");
        givenDocuments(document);

        final List<DBObject> result = underTest.getMonitoredApplicationsRaw();

        assertEquals(1, result.size());
        assertEquals("value", result.get(0).get("extra"));
    }

    @Test void returnsAnEmptyListOfRawDocumentsWhenNoApplicationIsMonitored() {
        givenDocuments();

        assertTrue(underTest.getMonitoredApplicationsRaw().isEmpty());
    }

    private void givenDocuments(DBObject... documents) {
        when(db.getCollection(MonitoredApplicationDaoImpl.COLL_APPLICATION_META_DATA)).thenReturn(collection);
        when(collection.find()).thenReturn(cursor);
        if(documents.length == 0) {
            when(cursor.hasNext()).thenReturn(false);
        }
        else if(documents.length == 1) {
            when(cursor.hasNext()).thenReturn(true, false);
            when(cursor.next()).thenReturn(documents[0]);
        }
        else {
            when(cursor.hasNext()).thenReturn(true, true, false);
            when(cursor.next()).thenReturn(documents[0], documents[1]);
        }
    }
}