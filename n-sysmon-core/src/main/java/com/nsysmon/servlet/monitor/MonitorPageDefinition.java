package com.nsysmon.servlet.monitor;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.servlet.timedscalars.AScalarDataPointValueComparator;
import com.nsysmon.servlet.timedscalars.TimedScalarsPageDefinition;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public class MonitorPageDefinition extends TimedScalarsPageDefinition {

    private volatile NSysMonApi sysMon;

    @Override
    public String getId() {
        return "monitoring";
    }

    @Override
    public String getShortLabel() {
        return "Monitor";
    }

    @Override
    public String getFullLabel() {
        return "Measurement Monitor";
    }

    @Override
    public String getHtmlFileName() {
        return "monitor.html";
    }

    @Override
    public String getControllerName() {
        return "CtrlMonitor";
    }

    @Override
    public void init(NSysMonApi sysMon) {
        this.sysMon = sysMon;
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) throws IOException {
        if ("getData".equals(service)) {
            serveData(json, params, sysMon);
            return true;
        } else if ("getMonitoringData".equals(service)) {
            serveMonitoringData(json, params);
            return true;
        }

        return false;
    }

    private void serveMonitoringData(final AJsonSerHelperForNSysmon json, List<String> params) throws IOException {
        String selectedEntries = params.get(0);

        json.startArray();
        for (String param : selectedEntries.split(",")) {
            String paramWithoutHtml = URLDecoder.decode(param, "UTF-8");
            final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurementsForMonitoring();

            for (String key : scalars.keySet()) {
                if (key.equalsIgnoreCase(paramWithoutHtml) && isMonitoringActive(key)) {
                    fillMonitoringData(json, scalars, key);
                }
            }
        }
        json.endArray();
    }

    private void fillMonitoringData(AJsonSerHelperForNSysmon json, Map<String, ARingBuffer<AScalarDataPoint>> scalars, String key) throws IOException {
        json.startObject();
        addMetainfos(json, key, sysMon);

        TreeSet<AScalarDataPoint> monitoringDataPoints = new TreeSet<>(new AScalarDataPointValueComparator());
        for (AScalarDataPoint dataPoint : scalars.get(key)) {
            monitoringDataPoints.add(dataPoint);
        }

        AScalarDataPoint max = monitoringDataPoints.stream().max(new AScalarDataPointValueComparator()).orElseGet(() -> null);
        json.writeKey("maxValue");
        if (max == null) {
            json.writeNumberLiteral(0, 0);
        } else {
            json.writeNumberLiteral(max.getValue(), max.getNumFracDigits());
        }

        AScalarDataPoint min = monitoringDataPoints.stream().min(new AScalarDataPointValueComparator()).orElseGet(() -> null);
        json.writeKey("minValue");
        if (min == null) {
            json.writeNumberLiteral(0, 0);
        } else {
            json.writeNumberLiteral(min.getValue(), min.getNumFracDigits());
        }

        json.writeKey("confMinValue");
        json.writeStringLiteral(getConfMinValue(key));

        json.writeKey("confMaxValue");
        json.writeStringLiteral(getConfMaxValue(key));

        AScalarDataPoint avg = getAverageValue(monitoringDataPoints);
        json.writeKey("avgValue");
        json.writeNumberLiteral(avg.getValue(), avg.getNumFracDigits());

        AScalarMeasurer.EvaluatedValue evaluatedValue = evaluateValue(key, avg.getValue());
        json.writeKey("threshold");
        json.writeStringLiteral(evaluatedValue.name());

        Long tsOldestMeasurement = getOldestTimestamp(monitoringDataPoints);
        json.writeKey("tsOldestMeasurement");
        json.writeNumberLiteral(tsOldestMeasurement, 0);

        Long tsLatestTimestamp = getLatestTimestamp(monitoringDataPoints);
        json.writeKey("tsLatestTimestamp");
        json.writeNumberLiteral(tsLatestTimestamp, 0);
//
        json.endObject();
    }

    private boolean isMonitoringActive(String key) {
        Optional<Map.Entry<String, Object>> value = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_ACTIVE))
                .filter(s -> s.getKey().contains(key))
                .findFirst();
        boolean rc = value.isPresent() && (Boolean) value.get().getValue();
        return rc;
    }

    private String getConfMinValue(String key) {
        Optional<Map.Entry<String, Object>> value = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_MEDIUM))
                .filter(s -> s.getKey().contains(key))
                .findFirst();

        if (value.isPresent()) {
            return "" + value.get().getValue();
        }
        return "???";
    }

    private String getConfMaxValue(String key) {
        Optional<Map.Entry<String, Object>> value = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_HIGH))
                .filter(s -> s.getKey().contains(key))
                .findFirst();

        if (value.isPresent()) {
            return "" + value.get().getValue();
        }
        return "???";
    }

    private AScalarMeasurer.EvaluatedValue evaluateValue(String key, double value) {
        Optional<Map.Entry<String, Object>> highValue = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_HIGH))
                .filter(s -> s.getKey().contains(key))
                .findFirst();
        if (highValue.isPresent() && ((Long) highValue.get().getValue()) < value) {
            return AScalarMeasurer.EvaluatedValue.HIGH;
        }

        Optional<Map.Entry<String, Object>> mediumValue = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_MEDIUM))
                .filter(s -> s.getKey().contains(key))
                .findFirst();
        if (mediumValue.isPresent() && ((Long) mediumValue.get().getValue()) < value) {
            return AScalarMeasurer.EvaluatedValue.MEDIUM;
        }

        return AScalarMeasurer.EvaluatedValue.LOW;
    }

    private AScalarDataPoint getAverageValue(TreeSet<AScalarDataPoint> filteredDataPoints) {
        AScalarDataPoint latestDataPoint = filteredDataPoints.last();

        final long[] sum = {0L};
        final long[] cnt = {0};
        filteredDataPoints.parallelStream().forEach(aScalarDataPoint -> {
            sum[0] += aScalarDataPoint.getValue();
            cnt[0]++;
        });

        sum[0] = (long) (sum[0] * Math.pow(10, latestDataPoint.getNumFracDigits() + 1));
        long value = sum[0] / cnt[0];
        return new AScalarDataPoint(latestDataPoint.getTimestamp(), latestDataPoint.getName(), value, latestDataPoint.getNumFracDigits() + 1);
    }

    private Long getOldestTimestamp(TreeSet<AScalarDataPoint> filteredDataPoints) {
        AScalarDataPoint dataPoint = filteredDataPoints.stream().min((o1, o2) -> {
            return (int) (o1.getTimestamp() - o2.getTimestamp());
        }).get();

        return dataPoint.getTimestamp();
    }

    private Long getLatestTimestamp(TreeSet<AScalarDataPoint> filteredDataPoints) {
        AScalarDataPoint dataPoint = filteredDataPoints.stream().max((o1, o2) -> {
            return (int) (o1.getTimestamp() - o2.getTimestamp());
        }).get();

        return dataPoint.getTimestamp();
    }

}
