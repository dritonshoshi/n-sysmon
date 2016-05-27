package com.nsysmon.servlet.monitor;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelper;
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

public class MonitorPageDefinition extends TimedScalarsPageDefinition{

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
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws IOException {
        if ("getData".equals(service)) {
            serveData(json, params, sysMon);
            return true;
        }else if ("getMonitoringData".equals(service)) {
            serveMonitoringData(json, params);
            return true;
        }

        return false;
    }

    private void serveMonitoringData(final AJsonSerHelper json, List<String> params) throws IOException {
        String selectedEntries = params.get(0);

        json.startArray();
        for (String param : selectedEntries.split(",")) {
            String paramWithoutHtml = URLDecoder.decode(param, "UTF-8");
            final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurementsForMonitoring();

            for (String key : scalars.keySet()) {
                if (key.equalsIgnoreCase(paramWithoutHtml)) {
                    fillMonitoringData(json, scalars, key);
                }
            }
        }
        json.endArray();
    }

    private void fillMonitoringData(AJsonSerHelper json, Map<String, ARingBuffer<AScalarDataPoint>> scalars, String key) throws IOException {
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
        }else {
            json.writeNumberLiteral(max.getValue(), max.getNumFracDigits());
        }

        AScalarDataPoint min = monitoringDataPoints.stream().min(new AScalarDataPointValueComparator()).orElseGet(() -> null);
        json.writeKey("minValue");
        if (min == null) {
            json.writeNumberLiteral(0, 0);
        }else {
            json.writeNumberLiteral(min.getValue(), min.getNumFracDigits());
        }

        AScalarDataPoint avg = getAverageValue(monitoringDataPoints);
        json.writeKey("avgValue");
        json.writeNumberLiteral(avg.getValue(), avg.getNumFracDigits());

        AScalarMeasurer.EvaluatedValue evaluatedValue = evaluateValue(key, avg.getValue());
        json.writeKey("threshold");
        json.writeStringLiteral(evaluatedValue.name());

        json.endObject();
    }

    private AScalarMeasurer.EvaluatedValue evaluateValue(String key, double value) {
        Optional<Map.Entry<String, Long>> highValue = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_HIGH))
                .filter(s -> s.getKey().contains(key))
                .findFirst();
        if (highValue.isPresent() && highValue.get().getValue() < value){
            return AScalarMeasurer.EvaluatedValue.HIGH;
        }

        Optional<Map.Entry<String, Long>> mediumValue = NSysMon.get().getConfig().getTimedScalarMonitoringParameters()
                .entrySet()
                .stream()
                .filter(s -> s.getKey().endsWith(AScalarMeasurer.KEY_CONFIGURATION_MEDIUM))
                .filter(s -> s.getKey().contains(key))
                .findFirst();
        if (mediumValue.isPresent() && mediumValue.get().getValue() < value){
            return AScalarMeasurer.EvaluatedValue.MEDIUM;
        }

        return AScalarMeasurer.EvaluatedValue.LOW;
    }

    private AScalarDataPoint getAverageValue(TreeSet<AScalarDataPoint> filteredDataPoints ){
        AScalarDataPoint latestDataPoint = filteredDataPoints.last();

        final long[] sum = {0L};
        final long[] cnt = {0};
        filteredDataPoints.parallelStream().forEach(aScalarDataPoint -> {
            sum[0] += aScalarDataPoint.getValue();
            cnt[0]++;
        });

        sum[0] = (long) (sum[0] * Math.pow(10, latestDataPoint.getNumFracDigits()+1));
        long value = sum[0] / cnt[0];
        return new AScalarDataPoint(latestDataPoint.getTimestamp(), latestDataPoint.getName(), value, latestDataPoint.getNumFracDigits()+1);
    }

}
