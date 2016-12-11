package com.nsysmon.datasink.transfer.types.db;

import com.nsysmon.data.ACorrelationId;

import java.util.Collection;

/**
 * Created by torsten on 11.12.2016.
 */
public class HierarchicalDataRootForStorage {
    private final Collection<ACorrelationId> joinedFlows;
    private final Collection<ACorrelationId> startedFlows;
    private final boolean killed;

    public HierarchicalDataRootForStorage(Collection<ACorrelationId> joinedFlows, Collection<ACorrelationId> startedFlows, boolean killed) {
        this.joinedFlows = joinedFlows;
        this.startedFlows = startedFlows;
        this.killed = killed;
    }
}
