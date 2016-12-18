package com.nsysmon.servlet.unfinished;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.servlet.longestcalls.HierarchicalDataDurationComparator;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

public class UnfinishedMeasurementsDataSink implements ADataSink {

    boolean isStarted = false;
    private final SortedSet<AHierarchicalDataRoot> sortedData;
    private final int maxBufferSize;
    private final boolean filterData;
    private final Set<String> entriesToIgnore;
    private final Pattern filterMatcher;

    UnfinishedMeasurementsDataSink(int bufferSize, boolean performStart, String findRegEx, Set<String> entriesToIgnore) {
        this.sortedData = new ConcurrentSkipListSet<>(new HierarchicalDataDurationComparator());
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
        this.sortedData.removeIf(next -> next.getUuid().equals(trace.getUuid()));
    }

    @Override
    public void onWorkingStep(AHierarchicalDataRoot trace) {
        this.sortedData.add(trace);
        System.out.println(trace);
        //TODO search old measurement
        //TODO remove it
        //TODO add new one
        //TODO must be very very fast! maybe even separate thread?
    }

    private boolean checkIfTraceShouldBeStored(AHierarchicalData trace) {
        for (String s : entriesToIgnore) {
            if (trace.getIdentifier().contains(s))
                return false;
        }
        return true;
    }

    private boolean storeAllCalls(AHierarchicalDataRoot trace) {
        if ((sortedData.size() > maxBufferSize) && (trace.getRootNode().getDurationNanos() <= sortedData.last().getRootNode().getDurationNanos())) {
            return false;
        } else {
            sortedData.add(trace);
            return true;
        }
    }

    public void clear() {
        sortedData.clear();
    }

    public Iterable<AHierarchicalDataRoot> getSortedData() {
        return new TreeSet<>(sortedData);
    }

    @Override
    public void shutdown() throws Exception {
    }

}
