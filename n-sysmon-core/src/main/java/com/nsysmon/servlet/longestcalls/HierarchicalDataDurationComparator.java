package com.nsysmon.servlet.longestcalls;

import com.nsysmon.data.AHierarchicalDataRoot;

import java.util.Comparator;

class HierarchicalDataDurationComparator implements Comparator<AHierarchicalDataRoot> {

    @Override
    public int compare(AHierarchicalDataRoot o1, AHierarchicalDataRoot o2) {
        if ((o1 == null) || (o1.getRootNode() == null)) {
            return -1;
        }
        if ((o2 == null) || (o2.getRootNode() == null)) {
            return 1;
        }
        if (o2.getRootNode().getDurationNanos() < o1.getRootNode().getDurationNanos()) {
            return -1;
        } else if (o2.getRootNode().getDurationNanos() > o1.getRootNode().getDurationNanos()) {
            return 1;
        } else if (o2.getRootNode().getDurationNanos() == o1.getRootNode().getDurationNanos()) {
            return 0;
        }
        return -1;
    }
}
