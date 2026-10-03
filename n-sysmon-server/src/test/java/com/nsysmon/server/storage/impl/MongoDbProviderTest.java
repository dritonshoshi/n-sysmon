package com.nsysmon.server.storage.impl;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class MongoDbProviderTest {
    private static final String DB_NAME = "n-sysmon";

    @Test void providesTheNsSysmonDatabase() {
        final DB expected = mock(DB.class);

        try(MockedConstruction<MongoClient> clients = mockMongoClientReturning(expected)) {
            final MongoDbProvider underTest = new MongoDbProvider();

            assertSame(expected, underTest.get());
            assertSame(expected, clients.constructed().get(0).getDB(DB_NAME));
        }
    }

    @Test void alwaysProvidesTheSameDatabaseInstance() {
        try(MockedConstruction<MongoClient> clients = mockMongoClientReturning(mock(DB.class))) {
            final MongoDbProvider underTest = new MongoDbProvider();

            assertSame(underTest.get(), underTest.get());
            assertSame(1, clients.constructed().size());
        }
    }

    /**
     * Replaces the legacy driver client so that no database connection is opened while the provider is constructed.
     */
    private static MockedConstruction<MongoClient> mockMongoClientReturning(DB db) {
        return mockConstruction(MongoClient.class, (client, context) -> when(client.getDB(DB_NAME)).thenReturn(db));
    }
}
