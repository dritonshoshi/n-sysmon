package com.nsysmon.server.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstanceIdentifierTest {
    @Test void exposesApplicationIdAndInstanceId() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertEquals("app-1", underTest.getApplicationId());
        assertEquals("instance-1", underTest.getInstanceId());
    }

    @Test void isEqualToAnotherIdentifierWithTheSameIds() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertEquals(new InstanceIdentifier("app-1", "instance-1"), underTest);
    }

    @Test void isEqualToItself() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertTrue(underTest.equals(underTest));
    }

    @Test void isNotEqualToIdentifierWithAnotherInstanceId() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertNotEquals(new InstanceIdentifier("app-1", "instance-2"), underTest);
    }

    @Test void isNotEqualToIdentifierWithAnotherApplicationId() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertNotEquals(new InstanceIdentifier("app-2", "instance-1"), underTest);
    }

    @Test void isNotEqualToNull() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertFalse(underTest.equals(null));
    }

    @Test void isNotEqualToObjectOfAnotherType() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertFalse(underTest.equals("app-1-instance-1"));
    }

    @Test void isEqualToIdentifierWithNullApplicationId() {
        final InstanceIdentifier underTest = new InstanceIdentifier(null, "instance-1");

        assertEquals(new InstanceIdentifier(null, "instance-1"), underTest);
    }

    @Test void isEqualToIdentifierWithNullIds() {
        final InstanceIdentifier underTest = new InstanceIdentifier(null, null);

        assertEquals(new InstanceIdentifier(null, null), underTest);
    }

    @Test void isNotEqualToIdentifierWithNullIds() {
        final InstanceIdentifier underTest = new InstanceIdentifier(null, null);

        assertNotEquals(new InstanceIdentifier("app-1", null), underTest);
    }

    @Test void isNotEqualToIdentifierWithApplicationIdOnlyOnTheOtherSide() {
        final InstanceIdentifier underTest = new InstanceIdentifier(null, "instance-1");

        assertFalse(underTest.equals(new InstanceIdentifier("app-1", "instance-1")));
    }

    @Test void isNotEqualToIdentifierWithInstanceIdOnlyOnTheOtherSide() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", null);

        assertFalse(underTest.equals(new InstanceIdentifier("app-1", "instance-1")));
    }

    @Test void hashCodeIsEqualForEqualIdentifiers() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertEquals(new InstanceIdentifier("app-1", "instance-1").hashCode(), underTest.hashCode());
    }

    @Test void hashCodeOfIdentifierWithNullIdsIsZero() {
        assertEquals(0, new InstanceIdentifier(null, null).hashCode());
    }

    @Test void toStringMentionsApplicationIdAndInstanceId() {
        final InstanceIdentifier underTest = new InstanceIdentifier("app-1", "instance-1");

        assertEquals("InstanceIdentifier{applicationId='app-1', instanceId='instance-1'}", underTest.toString());
    }
}
