package com.nsysmon.servlet.threaddump;

import com.nsysmon.testutil.MonitorDeadlock;
import org.junit.jupiter.api.Test;

import java.lang.management.ThreadInfo;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AThreadDumperTest {

    private static List<String> threadNames() {
        final List<String> result = new ArrayList<>();
        for(ThreadInfo ti: AThreadDumper.getThreadInfo()) {
            result.add(ti.getThreadName());
        }
        return result;
    }

    @Test void threadInfoContainsTheCurrentThread() {
        assertTrue(AThreadDumper.getThreadInfo().stream()
                .anyMatch(ti -> ti.getThreadName().equals(Thread.currentThread().getName())));
    }

    @Test void threadInfoIsSortedByName() {
        final List<String> names = threadNames();
        final Collator collator = Collator.getInstance();

        for (int i = 1; i < names.size(); i++) {
            assertTrue(collator.compare(names.get(i - 1), names.get(i)) <= 0, names.toString());
        }
    }

    @Test void threadInfoHasNoDuplicates() {
        final Collection<ThreadInfo> threads = AThreadDumper.getThreadInfo();

        assertEquals(threads.size(), new HashSet<>(threads).size());
    }

    @Test void noThreadIsDeadlockedInAHealthyJvm() {
        final Set<Long> deadlocked = Set.copyOf(AThreadDumper.getDeadlockedThreads());

        assertFalse(deadlocked.contains(Thread.currentThread().getId()));
    }

    @Test void reportsRealMonitorDeadlocks() {
        final List<Thread> threads = MonitorDeadlock.create("deadlock-a", "deadlock-b");

        final Set<Long> deadlocked = Set.copyOf(AThreadDumper.getDeadlockedThreads());

        assertTrue(deadlocked.contains(threads.get(0).getId()));
        assertTrue(deadlocked.contains(threads.get(1).getId()));
    }
}
