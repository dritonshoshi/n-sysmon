package com.nsysmon.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;


/**
 * @author arno
 */
public class AHierarchicalDataRoot {
    private final UUID uuid = UUID.randomUUID();
    private final Collection<ACorrelationId> startedFlows;
    private final Collection<ACorrelationId> joinedFlows;
    private final AHierarchicalData root;
    private final boolean wasKilled;

    public AHierarchicalDataRoot(AHierarchicalData root, Collection<ACorrelationId> startedFlows, Collection<ACorrelationId> joinedFlows) {
        this(root, startedFlows, joinedFlows, false);
    }

    public AHierarchicalDataRoot(AHierarchicalData root, Collection<ACorrelationId> startedFlows, Collection<ACorrelationId> joinedFlows, boolean wasKilled) {
        this.startedFlows = new ArrayList<>(startedFlows);
        this.joinedFlows = new ArrayList<>(joinedFlows);
        this.root = root;
        this.wasKilled = wasKilled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Collection<ACorrelationId> getStartedFlows() {
        return startedFlows;
    }

    public Collection<ACorrelationId> getJoinedFlows() {
        return joinedFlows;
    }

    public AHierarchicalData getRootNode() {
        return root;
    }

    public boolean isKilled() {
        return wasKilled;
    }

    @Override
    public String toString() {
        return "AHierarchicalDataRoot{" +
                "startedFlows=" + startedFlows +
                ", joinedFlows=" + joinedFlows +
                ", root=" + root +
                ", wasKilled=" + wasKilled +
                '}';
    }
}
