package com.nsysmon.testutil;

import com.nsysmon.util.timer.ATimer;

/**
 * @author arno
 */
public class ExplicitTimer implements ATimer {
    public long curNanos = 0;

    @Override public long getCurrentNanos() {
        return curNanos;
    }
}
