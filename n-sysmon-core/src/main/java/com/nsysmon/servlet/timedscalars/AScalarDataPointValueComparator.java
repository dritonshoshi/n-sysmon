package com.nsysmon.servlet.timedscalars;

import com.nsysmon.data.AScalarDataPoint;

import java.util.Comparator;

public class AScalarDataPointValueComparator implements Comparator<AScalarDataPoint>{
    @Override
    public int compare(AScalarDataPoint o1, AScalarDataPoint o2) {
        return (int) (o1.getValue()-o2.getValue());
    }
}
