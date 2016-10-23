package com.nsysmon.servlet.longestcalls;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

public class LongestCallsDataSink implements ADataSink {

    boolean isStarted = false;
    private final SortedSet<AHierarchicalDataRoot> data;
    private final int maxBufferSize;
    private final boolean filterData;
    private final Set<String> entriesToIgnore;
    private final Pattern filterMatcher;

    LongestCallsDataSink(int bufferSize, boolean performStart, String findRegEx, Set<String> entriesToIgnore) {
        this.data = new ConcurrentSkipListSet<>(new HierarchicalDataDurationComparator());
        this.isStarted = performStart;
        this.maxBufferSize = bufferSize;
        this.entriesToIgnore = entriesToIgnore;
        this.filterData = !findRegEx.isEmpty();

        filterMatcher = Pattern.compile(findRegEx.trim(), Pattern.CASE_INSENSITIVE);
    }

    @Override
    public void onStartedHierarchicalMeasurement(String identifier) {
    }

    @Override
    public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot trace) {
        if (!isStarted) {
            return;
        }

        boolean callStored = filterData ? storeRcCalls(trace) : storeAllCalls(trace);
        //callStored &= checkIfTraceShouldBeStored(trace);

        //remove entries if needed, only if we added a new entry. if we did not add a new entry, we don't have to check anything here
        if (callStored) {
            while (data.size() > maxBufferSize) {
                data.remove(data.last());
            }
        }
    }

    private boolean checkIfTraceShouldBeStored(AHierarchicalData trace) {
        for (String s : entriesToIgnore) {
            if (trace.getIdentifier().contains(s))
                return false;
        }
        return true;
    }

    private boolean storeAllCalls(AHierarchicalDataRoot trace) {
        if ((data.size() > maxBufferSize) && (trace.getRootNode().getDurationNanos() <= data.last().getRootNode().getDurationNanos())) {
            return false;
        } else {
            data.add(trace);
            return true;
        }
    }

    private boolean storeRcCalls(AHierarchicalDataRoot trace) {
        final List<AHierarchicalData> rcServiceCalls = new ArrayList<>();
        collectRcServiceCalls(trace.getRootNode(), rcServiceCalls);

        boolean callStored = false;
        for (AHierarchicalData current : rcServiceCalls) {
            this.data.add(new AHierarchicalDataRoot(current, trace.getStartedFlows(), trace.getJoinedFlows()));
            callStored = true;
        }
        return callStored;
    }

    private boolean collectRcServiceCalls(AHierarchicalData node, List<AHierarchicalData> rcServiceCalls) {
        if (filterMatcher.matcher(node.getIdentifier()).matches()) {
            if ((data.size() <= maxBufferSize) || (node.getDurationNanos() > data.last().getRootNode().getDurationNanos())) {
                if (checkIfTraceShouldBeStored(node)) {
                    rcServiceCalls.add(node);
                }
                return true;
            }
        } else {
            for (AHierarchicalData child : node.getChildren()) {
                collectRcServiceCalls(child, rcServiceCalls);
                //  final boolean rcServiceCallAdded = collectRcServiceCalls(child, rcServiceCalls);
                //if (rcServiceCallAdded) {
                //return true;
                //}
            }
        }
        return false;
    }

    public void clear() {
        data.clear();
    }

    public Iterable<AHierarchicalDataRoot> getData() {
        return new TreeSet<>(data);
    }

    @Override
    public void shutdown() throws Exception {
    }

}
