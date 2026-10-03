package com.nsysmon.server.storage.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.storage.MonitoredApplicationDao;
import com.nsysmon.server.storage.ScalarDataPoint;
import com.nsysmon.server.storage.ScalarMetaData;
import com.nsysmon.server.storage.ScalarMetaDataDao;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScalarDataDaoImplTest {
    private static final String APP_ID = "app-1";

    @Mock private DB db;
    @Mock private DBCollection coll;
    @Mock private MonitoredApplicationDao monitoredApplicationDao;
    @Mock private ScalarMetaDataDao scalarMetaDataDao;

    private ScalarDataDaoImpl underTest;

    @BeforeEach void createDao() {
        underTest = new ScalarDataDaoImpl(db, monitoredApplicationDao, scalarMetaDataDao);
    }

    @Test void storeScalarDataInsertsADocumentWithAllFields() {
        when(db.getCollection("app-1-_-cpu.load")).thenReturn(coll);

        underTest.storeScalarData(scalarNode());

        final ArgumentCaptor<DBObject> captor = ArgumentCaptor.forClass(DBObject.class);
        verify(coll).insert(captor.capture());
        assertEquals("scalar-uuid", captor.getValue().get("_id"));
        assertEquals("instance-1", captor.getValue().get("instance"));
        assertEquals(20L, captor.getValue().get("timestamp"));
        assertEquals(10L, captor.getValue().get("sender-timestamp"));
        assertEquals(0.75, captor.getValue().get("value"));
    }

    @Test void storeScalarDataUsesOneCollectionPerApplicationAndScalar() {
        when(db.getCollection("app-1-_-cpu.load")).thenReturn(coll);

        underTest.storeScalarData(scalarNode());

        verify(db).getCollection("app-1-_-cpu.load");
    }

    @Test void storeScalarDataIgnoresDuplicateKeys() {
        when(db.getCollection("app-1-_-cpu.load")).thenReturn(coll);
        when(coll.insert(any(DBObject.class))).thenThrow(mock(MongoException.DuplicateKey.class));

        underTest.storeScalarData(scalarNode());

        verify(coll).insert(any(DBObject.class));
    }

    @Test void getScalarCollectionCombinesApplicationIdAndScalarName() {
        when(db.getCollection("app-1-_-cpu.load")).thenReturn(coll);

        assertEquals(coll, underTest.getScalarCollection(new InstanceIdentifier(APP_ID, "instance-1"), "cpu.load"));
    }

    @Test void findAllQueriesTheCollectionOfTheGivenApplicationAndScalar() {
        givenDocumentsInCursor();

        underTest.findAll(APP_ID, "cpu.load", 10L, 20L, "instance-1");

        verify(db).getCollection("app-1-_-cpu.load");
    }

    @Test void findAllQueriesAnInclusiveTimestampRangeForTheGivenInstances() {
        givenDocumentsInCursor();

        underTest.findAll(APP_ID, "cpu.load", 10L, 20L, "instance-1", "instance-2");

        final ArgumentCaptor<DBObject> captor = ArgumentCaptor.forClass(DBObject.class);
        verify(coll).find(captor.capture());
        assertEquals(new BasicDBObject("$gte", 10L).append("$lte", 20L), captor.getValue().get("timestamp"));
        assertArrayEquals(new String[]{"instance-1", "instance-2"},
                (String[]) ((DBObject) captor.getValue().get("instance")).get("$in"));
    }

    @Test void findAllSortsByTimestampAndInstance() {
        final DBCursor cursor = givenDocumentsInCursor();

        underTest.findAll(APP_ID, "cpu.load", 10L, 20L, "instance-1");

        verify(cursor).sort(new BasicDBObject("timestamp", 1).append("instance", 1));
    }

    @Test void findAllMapsDocumentsToDataPoints() {
        givenDocumentsInCursor(new BasicDBObject("instance", "instance-1").append("timestamp", 15L).append("value", 0.5));

        final List<ScalarDataPoint> result = underTest.findAll(APP_ID, "cpu.load", 10L, 20L, "instance-1");

        assertEquals(1, result.size());
        assertEquals("instance-1", result.get(0).instance);
        assertEquals(15L, result.get(0).timestamp);
        assertEquals(0.5, result.get(0).value, 0.0);
    }

    @Test void findAllIsEmptyWhenNothingMatches() {
        givenDocumentsInCursor();

        assertTrue(underTest.findAll(APP_ID, "cpu.load", 10L, 20L, "instance-1").isEmpty());
    }

    @Test void initSchemaCreatesIndexOnEmptyScalarCollections() {
        givenScalarMetaData(0L, "cpu.load");

        underTest.initSchema();

        verify(coll).ensureIndex(new BasicDBObject("timestamp", 1).append("instance", 1),
                ScalarDataDaoImpl.INDEX_NAME_SCALAR_INSTANCE_AND_TIME);
    }

    @Test void initSchemaDoesNotCreateIndexOnExistingScalarCollections() {
        givenScalarMetaData(10L, "cpu.load");

        underTest.initSchema();

        verify(coll, never()).ensureIndex(any(DBObject.class), anyString());
    }

    @Test void initSchemaIteratesOverAllCombinationsOfApplicationAndScalar() {
        givenScalarMetaData(10L, "cpu.load", "heap.used");

        underTest.initSchema();

        verify(db).getCollection("app-1-_-cpu.load");
        verify(db).getCollection("app-1-_-heap.used");
    }

    private void givenScalarMetaData(long collectionCount, String... names) {
        final Map<String, ScalarMetaData> metaData = new HashMap<>();
        for(String name: names) {
            metaData.put(name, new ScalarMetaData(name, 1));
        }
        when(monitoredApplicationDao.getMonitoredApplicationNames()).thenReturn(List.of(APP_ID));
        when(scalarMetaDataDao.getAll()).thenReturn(metaData);
        when(db.getCollection(anyString())).thenReturn(coll);
        when(coll.count()).thenReturn(collectionCount);
    }

    private DBCursor givenDocumentsInCursor(DBObject... documents) {
        final DBCursor cursor = mock(DBCursor.class);
        when(cursor.iterator()).thenReturn(Arrays.asList(documents).iterator());
        when(cursor.sort(any(DBObject.class))).thenReturn(cursor);
        when(db.getCollection("app-1-_-cpu.load")).thenReturn(coll);
        when(coll.find(any(DBObject.class))).thenReturn(cursor);
        return cursor;
    }

    private static ScalarNode scalarNode() {
        final ScalarNode result = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);
        result.setInstanceIdentifier(new InstanceIdentifier(APP_ID, "instance-1"));
        result.setAdjustedTimestamp(20L);
        return result;
    }
}