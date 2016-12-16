package com.nsysmon.datasink.transfer.types.db;

import java.util.Map;

/**
 * Created by torsten on 11.12.2016.
 */
public class HierarchicalDataForStorage {
    private String _id;
    private final String identifier;
    private final long startTimeMillis;
    private final Map<String, String> parameters;
    private final boolean hasChildren;
    private final long level;
    private final long durationNanos;
    private final String localIdentifier;
    private final String parentIdentifier;//TODO
    private String idRoot;

    public HierarchicalDataForStorage(String identifier, long startTimeMillis, long durationNanos, Map<String, String> parameters, boolean hasChildren, long level, String localIdentifier, String parentIdentifier, String idRoot) {
        this.identifier = identifier;
        this.startTimeMillis = startTimeMillis;
        this.parameters = parameters;
        this.hasChildren = hasChildren;
        this.level = level;
        this.localIdentifier = localIdentifier;
        this.parentIdentifier = parentIdentifier;
        this.idRoot = idRoot;
        this.durationNanos = durationNanos;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public void setIdRoot(String idRoot) {
        this.idRoot = idRoot;
    }

    @Override
    public String toString() {
        return "HierarchicalDataForStorage{" +
                "identifier='" + identifier + '\'' +
                ", startTimeMillis=" + startTimeMillis +
                ", durationNanos=" + durationNanos +
                ", parameters=" + parameters +
                ", hasChildren=" + hasChildren +
                ", level=" + level +
                ", localIdentifier='" + localIdentifier + '\'' +
                ", parentIdentifier='" + parentIdentifier + '\'' +
                ", idRoot='" + idRoot + '\'' +
                '}';
    }
}
