package com.nsysmon.data;


import java.util.*;


/**
 * @author arno
 */
public class AHierarchicalData {
    private final boolean isSerial;

    private final long startTimeMillis;
    private final long durationNanos;
    private final String identifier;
    private final boolean wasKilled;

    private final Map<String, String> parameters;
    private final List<AHierarchicalData> children;

    /**
     * @param isSerial <code>true</code> designates the intuitive situation that a parent 'contains' several non-overlapping measurements,
     *                     with the sum of the children's durations being less than or equals to the parent's duration and the difference
     *                     being spent in the parent itself.<p>
     *                     <code>false</code> designates more exotic measurements that may 'overlap' other measurements etc., providing
     *                     additional data but not being an additive part of the parent's duration. This could e.g. be the delay between
     *                     sending an asynchronous request and receiving the response.
     * @param identifier is used for aggregated rendering of results - measurements with equal identifiers are treated
     *                   as 'equivalent'.
     */
    public AHierarchicalData(boolean isSerial, long startTimeMillis, long durationNanos, String identifier, Map<String, String> parameters, List<AHierarchicalData> children, final boolean wasKilled) {
        this.isSerial = isSerial;
        this.startTimeMillis = startTimeMillis;
        this.durationNanos = durationNanos;
        this.identifier = identifier.intern();

        //use empty 0-size map or null for better memory management
        if (parameters.size() == 0){
            this.parameters = null;
        } else {
            Map<String, String> tmpParameters = new HashMap<>(0);
            tmpParameters.putAll(parameters);
            this.parameters = tmpParameters;
        }

        //TODO check if it is save to change this to local array with new instance like below
        // might be a problem, because the caller uses the children for other things, look out for childrenStack in caller-methods
        //use empty 0-size map or null for better memory management
//        if (children.size() == 0 && this.children == null){
//            this.children = null;
//        } else {
//            ArrayList<AHierarchicalData> tmpChildren = new ArrayList<>(0);
//            tmpChildren.addAll(children);
//            tmpChildren.trimToSize();
//            this.children = tmpChildren;
//        }

        this.children = children;
        this.wasKilled = wasKilled;
    }

    public boolean isSerial() {
        return isSerial;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, String> getParameters() {
        return parameters == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(parameters);
    }

    public List<AHierarchicalData> getChildren() {
        return children == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(children);
    }

    //TODO TKT add this to rest
    public boolean isWasKilled() {
        return wasKilled;
    }

    @Override
    public String toString() {
        return "AHierarchicalData {" + identifier + " " + parameters + " : " + children + " " + wasKilled + "}";
    }
}
