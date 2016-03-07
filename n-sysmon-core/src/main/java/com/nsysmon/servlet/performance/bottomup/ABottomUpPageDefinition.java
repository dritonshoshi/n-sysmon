package com.nsysmon.servlet.performance.bottomup;

import com.nsysmon.NSysMonApi;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.servlet.performance.AAbstractNsysmonPerformancePageDef;
import com.nsysmon.servlet.performance.AMinMaxAvgData;

import java.text.Collator;
import java.util.*;

/**
 * @author arno
 */
public abstract class ABottomUpPageDefinition extends AAbstractNsysmonPerformancePageDef {
    private volatile ABottomUpDataSink collector;

    public static final List<ColDef> COL_DEFS = Arrays.asList(
            new ColDef("%", true, 1, ColWidth.Medium),
            new ColDef("%local", false, 1, ColWidth.Medium),
            new ColDef("#calls", false, 0, ColWidth.Medium),
            new ColDef("avg ms", false, 0, ColWidth.Medium),
            new ColDef("min ms", false, 0, ColWidth.Medium),
            new ColDef("max ms", false, 0, ColWidth.Medium)
    );

    public static final int MILLION = 1000*1000;

    protected abstract ABottomUpLeafFilter createLeafFilter();

    @Override public void init(NSysMonApi sysMon) {
        super.init(sysMon);
        collector = new ABottomUpDataSink(createLeafFilter());
        NSysMonConfigurer.addDataSink(sysMon, collector);
    }

    @Override protected boolean isStarted() {
        return collector.isActive();
    }

    @Override protected void doStartMeasurements() {
        collector.setActive(true);
    }

    @Override protected void doStopMeasurements() {
        collector.setActive(false);
    }

    @Override protected void doClearMeasurements() {
        collector.clear();
    }

    @Override protected List<ColDef> getColDefs() {
        return COL_DEFS;
    }

    @Override protected List<TreeNode> getData() {
        long totalJdbcNanos = 0;
        int totalJdbcCalls = 0;
        for(AMinMaxAvgData d: collector.getData().values()) {
            totalJdbcNanos += d.getTotalNanos();
            totalJdbcCalls += d.getTotalNumInContext();
        }

        return getDataRec(collector.getData(), 0, totalJdbcNanos, totalJdbcNanos, totalJdbcCalls);
    }

    private List<TreeNode> getDataRec(Map<String, AMinMaxAvgData> map, int level, double jdbcTimeInParent, double totalJdbcTime, int totalNumCallsInContext) {
        final List<TreeNode> result = new ArrayList<>();

        for(Map.Entry<String, AMinMaxAvgData> entry: getSorted(map, level == 0)) {
            final AMinMaxAvgData inputData = entry.getValue();

            final double jdbcTimeHere = (level == 0) ? inputData.getTotalNanos() : jdbcTimeInParent * inputData.getTotalNumInContext() / totalNumCallsInContext;
            final double timeFracLocal = jdbcTimeHere / jdbcTimeInParent;
            final double timeFracGlobal = jdbcTimeHere / totalJdbcTime;

            final long[] dataRaw = new long[] {
                    (long)(timeFracGlobal * 100 * 10),
                    (long)(timeFracLocal * 100 * 10),
                    inputData.getTotalNumInContext(),
                    inputData.getAvgNanos() / MILLION,
                    inputData.getMinNanos() / MILLION,
                    inputData.getMaxNanos() / MILLION
            };

            int totalChildCalls = 0;
            for(AMinMaxAvgData childData: inputData.getChildren().values()) {
                totalChildCalls += childData.getTotalNumInContext();
            }
            // TODO: Add Tooltip to Treenode. @See: ATracePageDefinition.class
            result.add(new TreeNode(entry.getKey(), inputData.isSerial(), dataRaw, getDataRec(inputData.getChildren(), level+1, jdbcTimeHere, totalJdbcTime, totalChildCalls)));
        }

        return result;
    }

    private List<Map.Entry<String, AMinMaxAvgData>> getSorted(Map<String, AMinMaxAvgData> raw, final boolean rootLevel) {
        final List<Map.Entry<String, AMinMaxAvgData>> result = new ArrayList<>(raw.entrySet());

        Collections.sort(result, (o1, o2) -> {
            final long delta = rootLevel ? (o2.getValue().getTotalNanos() - o1.getValue().getTotalNanos()) : (o2.getValue().getTotalNumInContext() - o1.getValue().getTotalNumInContext());
            if (delta > 0) {
                return 1;
            }
            if (delta < 0) {
                return -1;
            }
            return Collator.getInstance().compare(o1.getKey(), o2.getKey());
        });
        return result;
    }
}
