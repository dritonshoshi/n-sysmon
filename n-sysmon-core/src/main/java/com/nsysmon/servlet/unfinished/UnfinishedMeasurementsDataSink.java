package com.nsysmon.servlet.unfinished;

import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.HierarchicalParentInfo;
import com.nsysmon.servlet.longestcalls.HierarchicalDataDurationComparator;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Pattern;

public class UnfinishedMeasurementsDataSink implements ADataSink {

    boolean isStarted = false;
    private final SortedSet<AHierarchicalDataRoot> sortedData;
    private final ConcurrentSkipListSet<TmpStorageStructure> cachedData;
    private final int maxBufferSize;
    private final boolean filterData;
    private final Set<String> entriesToIgnore;
    private final Pattern filterMatcher;

    private class TmpStorageStructure{
        private final AHierarchicalDataRoot trace;
        private final HierarchicalParentInfo parentInfo;
        TmpStorageStructure(AHierarchicalDataRoot trace, HierarchicalParentInfo parentInfo){
            this.trace = trace;
            this.parentInfo = parentInfo;
        }

        public AHierarchicalDataRoot getTrace() {
            return trace;
        }

        public HierarchicalParentInfo getParentInfo() {
            return parentInfo;
        }
    }

    UnfinishedMeasurementsDataSink(int bufferSize, boolean performStart, String findRegEx, Set<String> entriesToIgnore) {
        this.sortedData = new ConcurrentSkipListSet<>(new HierarchicalDataDurationComparator());
        this.cachedData = new ConcurrentSkipListSet<>(new Comparator<TmpStorageStructure>() {
            @Override
            public int compare(TmpStorageStructure o1, TmpStorageStructure o2) {
                //TODO
                return o1.getParentInfo().getIdentifier().compareTo(o2.getParentInfo().getIdentifier());
            }
        });
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
        //TODO this.cachedData.removeIf(next -> next.getUuid().equals(trace.getUuid()));
    }

    @Override
    public void onWorkingStep(AHierarchicalDataRoot trace, HierarchicalParentInfo parentInfo) {
        this.cachedData.add(new TmpStorageStructure(trace, parentInfo));
        System.out.println(trace);
        //TODO search old measurement
        //TODO remove it
        //TODO add new one
        //TODO must be very very fast! maybe even separate thread?
    }

//    private boolean checkIfTraceShouldBeStored(AHierarchicalData trace) {
//        for (String s : entriesToIgnore) {
//            if (trace.getIdentifier().contains(s))
//                return false;
//        }
//        return true;
//    }
//
//    private boolean storeAllCalls(AHierarchicalDataRoot trace) {
//        if ((cachedData.size() > maxBufferSize) && (trace.getRootNode().getDurationNanos() <= cachedData.last().getRootNode().getDurationNanos())) {
//            return false;
//        } else {
//            cachedData.add(trace);
//            return true;
//        }
//    }

    public void clear() {
        cachedData.clear();
    }

    public Iterable<AHierarchicalDataRoot> getSortedData() {
        return createTree(cachedData);
    }

    private TreeSet<AHierarchicalDataRoot> createTree(ConcurrentSkipListSet<TmpStorageStructure> cachedData) {
        HashSet<AHierarchicalDataRoot> rootItems = new HashSet<>();

        for (TmpStorageStructure cachedDatum : cachedData) {
            if (cachedDatum.getParentInfo().isTopEntry()){
                System.out.println(cachedDatum.getParentInfo().getIdentifier());
                System.out.println("\t" + cachedDatum.getTrace().getRootNode().getIdentifier());
            }
        }
        return new TreeSet<>(sortedData);
    }

    @Override
    public void shutdown() throws Exception {
    }

}
