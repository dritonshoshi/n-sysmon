package com.nsysmon.datasink.transfer.types.db;

import java.util.Map;

/**
 * Created by torsten on 11.12.2016.
 */
public class HierarchicalDataForStorage {
    private final String identifier;
    private final long startTimeMillis;
    private final Map<String, String> parameters;
    private final boolean empty;
    private final long level;
    private final String localIdentifier;
    private final String parentIdentifier;

    public HierarchicalDataForStorage(String identifier, long startTimeMillis, Map<String, String> parameters, boolean empty, long level, String localIdentifier, String parentIdentifier) {
        this.identifier = identifier;
        this.startTimeMillis = startTimeMillis;
        this.parameters = parameters;
        this.empty = empty;
        this.level = level;
        this.localIdentifier = localIdentifier;
        this.parentIdentifier = parentIdentifier;
    }

    @Override
    public String toString() {
        return "HierarchicalDataForStorage{" +
                "identifier='" + identifier + '\'' +
                ", startTimeMillis=" + startTimeMillis +
                ", parameters=" + parameters +
                ", empty=" + empty +
                ", level=" + level +
                ", localIdentifier='" + localIdentifier + '\'' +
                ", parentIdentifier='" + parentIdentifier + '\'' +
                '}';
    }
}
