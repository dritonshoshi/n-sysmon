package com.nsysmon.servlet.correlationflow;

import com.nsysmon.data.ACorrelationId;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class ACorrelationIdComparator implements Comparator<Map.Entry<ACorrelationId, Set<ACorrelationId>>> {

    @Override public int compare(Map.Entry<ACorrelationId, Set<ACorrelationId>> o1, Map.Entry<ACorrelationId, Set<ACorrelationId>> o2) {
        if (o1 == null && o2 == null){
            return 0;
        }
        if (o1 == null && o2 != null){
            return 1;
        }
        if (o1.getKey() != null && o2.getKey() == null){
            return -1;
        }
        if (o1.getKey() == null && o2.getKey() == null){
            return 0;
        }
        if (o1.getKey() == null && o2.getKey() != null){
            return 1;
        }
        if (o1.getKey() != null && o2.getKey() == null){
            return -1;
        }
        if (o1.getKey().getQualifier() == null){
            return -1;
        }
        return o1.getKey().getQualifier().compareTo(o2.getKey().getQualifier());
    }
}
