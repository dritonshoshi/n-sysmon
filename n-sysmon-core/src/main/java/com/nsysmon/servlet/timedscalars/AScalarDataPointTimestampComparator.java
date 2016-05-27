package com.nsysmon.servlet.timedscalars;

import com.nsysmon.data.AScalarDataPoint;

import java.util.Comparator;

public class AScalarDataPointTimestampComparator implements Comparator<AScalarDataPoint>{
    @Override
    public int compare(AScalarDataPoint o1, AScalarDataPoint o2) {
        if (o1.getTimestamp() > o2.getTimestamp()){
            return 1;
        }
        if (o1.getTimestamp() < o2.getTimestamp()){
            return -1;
        }
        return 0;
    }
}
