package com.nsysmon.datasink;

import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.util.AShutdownable;

/**
 * @author arno
 */
public interface ADataSink extends AShutdownable {
    void onStartedHierarchicalMeasurement(String identifier);
    void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data);
}
