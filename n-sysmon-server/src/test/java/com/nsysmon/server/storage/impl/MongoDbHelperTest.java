package com.nsysmon.server.storage.impl;

import com.mongodb.BasicDBObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MongoDbHelperTest {
    @Test void separatorIsUnderscoredAndDash() {
        assertEquals("-_-", MongoDbHelper.SEPARATOR);
    }

    @Test void canBeInstantiatedEvenThoughItOnlyOffersStaticMethods() {
        assertNotNull(new MongoDbHelper());
    }

    @Test void asIntExpectedReturnsTheIntValueOfANumber() {
        assertEquals(42, MongoDbHelper.asIntExpected(new BasicDBObject("field", 42L), "field"));
    }

    @Test void asIntExpectedFailsForAMissingField() {
        assertThrows(NullPointerException.class, () -> MongoDbHelper.asIntExpected(new BasicDBObject(), "field"));
    }

    @Test void asIntReturnsTheDefaultForAMissingField() {
        assertEquals(7, MongoDbHelper.asInt(new BasicDBObject(), "field", 7));
    }

    @Test void asIntReturnsTheStoredValue() {
        assertEquals(42, MongoDbHelper.asInt(new BasicDBObject("field", 42L), "field", 7));
    }

    @Test void asLongExpectedReturnsTheLongValueOfANumber() {
        assertEquals(42L, MongoDbHelper.asLongExpected(new BasicDBObject("field", 42), "field"));
    }

    @Test void asLongExpectedFailsForAMissingField() {
        assertThrows(NullPointerException.class, () -> MongoDbHelper.asLongExpected(new BasicDBObject(), "field"));
    }

    @Test void asLongReturnsTheDefaultForAMissingField() {
        assertEquals(7L, MongoDbHelper.asLong(new BasicDBObject(), "field", 7L));
    }

    @Test void asLongReturnsTheStoredValue() {
        assertEquals(42L, MongoDbHelper.asLong(new BasicDBObject("field", 42), "field", 7L));
    }

    @Test void asDoubleExpectedReturnsTheDoubleValueOfANumber() {
        assertEquals(0.5, MongoDbHelper.asDoubleExpected(new BasicDBObject("field", 0.5f), "field"), 0.0);
    }

    @Test void asDoubleExpectedFailsForAMissingField() {
        assertThrows(NullPointerException.class, () -> MongoDbHelper.asDoubleExpected(new BasicDBObject(), "field"));
    }

    @Test void asDoubleReturnsTheDefaultForAMissingField() {
        assertEquals(0.5, MongoDbHelper.asDouble(new BasicDBObject(), "field", 0.5), 0.0);
    }

    @Test void asDoubleReturnsTheStoredValue() {
        assertEquals(42.0, MongoDbHelper.asDouble(new BasicDBObject("field", 42L), "field", 0.5), 0.0);
    }

    @Test void createQueryTimestampRangeCreatesAnInclusiveRange() {
        assertEquals(new BasicDBObject("$gte", 10L).append("$lte", 20L),
                MongoDbHelper.createQueryTimestampRange(10L, 20L));
    }

    @Test void createQueryInClauseWrapsTheStrings() {
        assertArrayEquals(new String[]{"a", "b"}, (String[]) MongoDbHelper.createQueryInClause("a", "b").get("$in"));
    }

    @Test void createQueryInClauseWithoutStringsCreatesAnEmptyInClause() {
        assertArrayEquals(new String[0], (String[]) MongoDbHelper.createQueryInClause().get("$in"));
    }

    @Test void createSortSpecSortsAscendingByAllGivenFields() {
        assertEquals(new BasicDBObject("timestamp", 1).append("instance", 1),
                MongoDbHelper.createSortSpec("timestamp", "instance"));
    }

    @Test void createSortSpecWithoutFieldsIsEmpty() {
        assertEquals(new BasicDBObject(), MongoDbHelper.createSortSpec());
    }
}