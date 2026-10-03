package com.nsysmon.impl;

import java.lang.reflect.Field;

/**
 * The robust wrappers keep their circuit-breaker state in a private field. Reading and seeding it is the
 * only way to drive the strategy transitions deterministically (their timing thresholds use
 * {@code System.nanoTime()} and cannot be controlled from a test).
 */
final class ImplTestSupport {

    private ImplTestSupport() {
    }

    /**
     * A timeout this large can never be exceeded, so the wrapper never takes the "timed out" branch.
     */
    static final long NEVER_TIMES_OUT = Long.MAX_VALUE;

    /**
     * A timeout this small is always exceeded, so the wrapper always takes the "timed out" branch.
     */
    static final long ALWAYS_TIMES_OUT = Long.MIN_VALUE;

    static Object read(Object target, String fieldName) {
        try {
            return fieldOf(target.getClass(), fieldName).get(target);
        } catch (IllegalAccessException exc) {
            throw new AssertionError("cannot read field " + fieldName, exc);
        }
    }

    static void write(Object target, String fieldName, Object value) {
        try {
            fieldOf(target.getClass(), fieldName).set(target, value);
        } catch (IllegalAccessException exc) {
            throw new AssertionError("cannot write field " + fieldName, exc);
        }
    }

    private static Field fieldOf(Class<?> type, String fieldName) {
        try {
            final Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exc) {
            throw new AssertionError("no such field " + type.getName() + "." + fieldName, exc);
        }
    }
}