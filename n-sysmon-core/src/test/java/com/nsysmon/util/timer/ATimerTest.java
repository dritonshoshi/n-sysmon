package com.nsysmon.util.timer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ATimerTest {

    private static final long ONE_MILLI_IN_NANOS = 1_000_000L;

    @Test
    void theNanoTimerImplementsTheTimerContract() {
        assertTrue(ATimer.class.isAssignableFrom(ASystemNanoTimer.class));
    }

    @Test
    void theMillisTimerImplementsTheTimerContract() {
        assertTrue(ATimer.class.isAssignableFrom(ASystemMillisTimer.class));
    }

    @Test
    void theNanoTimerReadsTheSystemClock() {
        final long before = System.nanoTime();
        final long nanos = new ASystemNanoTimer().getCurrentNanos();
        final long after = System.nanoTime();

        assertTrue(nanos >= before && nanos <= after, nanos + " not within [" + before + ", " + after + "]");
    }

    @Test
    void theNanoTimerDoesNotGoBackwards() {
        final ASystemNanoTimer timer = new ASystemNanoTimer();

        final long first = timer.getCurrentNanos();
        final long second = timer.getCurrentNanos();

        assertTrue(second >= first, second + " < " + first);
    }

    @Test
    void theMillisTimerScalesMillisToNanos() {
        final long before = System.currentTimeMillis();

        final long nanos = new ASystemMillisTimer().getCurrentNanos();

        assertEquals(0L, nanos % ONE_MILLI_IN_NANOS);
        assertTrue(nanos >= before * ONE_MILLI_IN_NANOS, nanos + " < " + before);
    }

    @Test
    void theMillisTimerDoesNotGoBackwards() {
        final ASystemMillisTimer timer = new ASystemMillisTimer();

        final long first = timer.getCurrentNanos();
        final long second = timer.getCurrentNanos();

        assertTrue(second >= first, second + " < " + first);
    }
}
