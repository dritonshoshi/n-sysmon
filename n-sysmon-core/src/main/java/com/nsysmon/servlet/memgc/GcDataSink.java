package com.nsysmon.servlet.memgc;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class remembers the most recent GC details, discarding older data to make room for newer data.
 *
 * @author arno
 */
class GcDataSink implements ADataSink {
    private final ARingBuffer<GcDetails> dataBuffer;

    GcDataSink(int maxNumDetails) {
        dataBuffer = new ARingBuffer<>(GcDetails.class, maxNumDetails);
    }

    @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        if(! AJmxGcMeasurer.IDENT_GC_TRACE_ROOT.equals(data.getRootNode().getIdentifier())) {
            return;
        }

        final Map<String, String> params = data.getRootNode().getParameters();
        for(AHierarchicalData d: data.getRootNode().getChildren()) {
            final long startMillis = d.getStartTimeMillis();
            final long durationNanos = d.getDurationNanos();
            final String gcType = d.getIdentifier();

            final String cause = params.get(AJmxGcMeasurer.KEY_CAUSE);
            final String algorithm = params.get(AJmxGcMeasurer.KEY_ALGORITHM);

            final GcDetails gcDetails = new GcDetails(startMillis, durationNanos, gcType, cause, algorithm);

            for(String memKind: memKinds(params.keySet())) {
                //TO_CONSIDER create a cache with the keys, here and in AJmxGcMeasurer?
                final long usedAfter = Long.valueOf(params.get(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_USED));
                final long committedAfter = Long.valueOf(params.get(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED));

                final long usedBefore = usedAfter - Long.valueOf(params.get(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_USED_DELTA));
                final long committedBefore = committedAfter - Long.valueOf(params.get(AJmxGcMeasurer.KEY_PREFIX_MEM + memKind + AJmxGcMeasurer.KEY_SUFFIX_COMMITTED_DELTA));

                gcDetails.memDetails.add(new GcMemDetails(memKind, usedBefore, usedAfter, committedBefore, committedAfter));
            }

            dataBuffer.put(gcDetails);
        }
    }

    private Set<String> memKinds (Collection<String> paramKeys) {
        final Set<String> result = new HashSet<>();

        for(String key: paramKeys) {
            if(! key.startsWith(AJmxGcMeasurer.KEY_PREFIX_MEM)) {
                continue;
            }

            if(key.endsWith(AJmxGcMeasurer.KEY_SUFFIX_USED)) {
                final String withoutPrefix = key.substring(AJmxGcMeasurer.KEY_PREFIX_MEM.length());
                result.add(withoutPrefix.substring(0, withoutPrefix.length() - AJmxGcMeasurer.KEY_SUFFIX_USED.length()));
            }
        }

        return result;
    }

    public Iterable<GcDetails> getData() {
        return dataBuffer;
    }

    @Override public void shutdown() throws Exception {
    }

    @Override public void onStartedHierarchicalMeasurement(String identifier) {
    }
}
