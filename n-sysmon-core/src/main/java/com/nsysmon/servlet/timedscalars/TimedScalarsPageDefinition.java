package com.nsysmon.servlet.timedscalars;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.servlet.overview.DataFileGeneratorSupporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


public class TimedScalarsPageDefinition implements APresentationPageDefinition, DataFileGeneratorSupporter {
    private volatile NSysMonApi sysMon;
    private static final NSysMonLogger LOG = NSysMonLogger.get(TimedScalarsPageDefinition.class);

    @Override
    public String getId() {
        return "timedScalars";
    }

    @Override
    public String getShortLabel() {
        return "TimedScalars";
    }

    @Override
    public String getFullLabel() {
        return "Timed Scalar Measurements";
    }

    @Override
    public String getHtmlFileName() {
        return "timedscalars.html";
    }

    @Override
    public String getControllerName() {
        return "CtrlTimedScalars";
    }

    @Override
    public void init(NSysMonApi sysMon) {
        this.sysMon = sysMon;
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws IOException {
        if ("getData".equals(service)) {
            serveData(json, params);
            return true;
        }else if ("getGraphData".equals(service)) {
            serveGraphData(json, params);
            return true;
        }else if ("getLatestGraphData".equals(service)) {
            serveLatestGraphData(json, params);
            return true;
        }else if ("getMonitoringData".equals(service)) {
            serveMonitoringData(json, params);
            return true;
        }

        return false;
    }

    private void serveGraphData(final AJsonSerHelper json, List<String> params) throws IOException {
        if (params == null || params.size() < 1){
            return;
        }
        String selectedEntries = params.get(0);

        json.startArray();
        for (String param : selectedEntries.split(",")) {
            String paramWithoutHtml = URLDecoder.decode(param, "UTF-8");
            final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();

            for (String key : scalars.keySet()) {
                if (key.equalsIgnoreCase(paramWithoutHtml)) {
                    json.startObject();
                    addMetainfos(json, key);

                    json.writeKey("values");
                    json.startArray();
                    writeRingBufferIntoJson(json, scalars.get(key));
                    json.endArray();
                    json.endObject();
                }
            }
        }
        json.endArray();

    }

    private void serveLatestGraphData(final AJsonSerHelper json, List<String> params) throws IOException {
        if (params == null || params.size() < 1){
            return;
        }
        String selectedEntries = params.get(0);
        Duration newerThan = Duration.ofMinutes(Long.parseLong(params.get(1)));


        json.startArray();
        for (String param : selectedEntries.split(",")) {
            String paramWithoutHtml = URLDecoder.decode(param, "UTF-8");
            final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();

            for (String key : scalars.keySet()) {
                if (key.equalsIgnoreCase(paramWithoutHtml)) {
                    json.startObject();
                    addMetainfos(json, key);

                    json.writeKey("values");
                    json.startArray();
                    writeRingBufferIntoJson(json, scalars.get(key), newerThan);
                    json.endArray();
                    json.endObject();
                }
            }
        }
        json.endArray();
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
        addMetainfos(json, key);

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

        //TODO FOX088S check if evaluation should be changed to double
        AScalarMeasurer.EvaluatedValue evaluatedValue = evaluateValue(key, avg.getValue());
        json.writeKey("threshold");
        json.writeStringLiteral(evaluatedValue.name());

        json.endObject();
    }

    private AScalarMeasurer.EvaluatedValue evaluateValue(String key, double value) {
        Set<String> configKeys = NSysMon.get().getConfig().getTimedScalarMonitoringParameters().keySet();
        Optional<String> highValue = configKeys.stream()
                .filter(s -> s.endsWith(AScalarMeasurer.KEY_CONFIGURATION_HIGH))
                .filter(s -> s.contains(key))
                .findFirst();
        final Long[] valueToCompare = new Long[1];
        valueToCompare[0] = 0L;
        highValue.ifPresent(s -> valueToCompare[0] = NSysMon.get().getConfig().getTimedScalarMonitoringParameters().get(s));
        if (valueToCompare[0] != 0L && valueToCompare[0] < value) {
            return AScalarMeasurer.EvaluatedValue.HIGH;
        }
        //check medium, if no high was detected
            Optional<String> mediumValue = configKeys.stream()
                    .filter(s -> s.endsWith(AScalarMeasurer.KEY_CONFIGURATION_MEDIUM))
                    .filter(s -> s.contains(key))
                    .findFirst();
            mediumValue.ifPresent(s -> valueToCompare[0] = NSysMon.get().getConfig().getTimedScalarMonitoringParameters().get(s));

        if (valueToCompare[0] != 0L && valueToCompare[0] < value) {
            return AScalarMeasurer.EvaluatedValue.MEDIUM;
        }
        return AScalarMeasurer.EvaluatedValue.LOW;
    }

    private void addMetainfos(AJsonSerHelper json, String key) throws IOException {
        json.writeKey("key");
        json.writeStringLiteral(key);

        String description = findDescriptionForKey(key);
        if (description != null) {
            json.writeKey("description");
            json.writeStringLiteral(description);
        }

        String group = findGroupForKey(key);
        if (group != null) {
            json.writeKey("group");
            json.writeStringLiteral(group);
        }
    }

    private String findGroupForKey(String key) {
        String tmp;
        for (AScalarMeasurer initialTimedScalarMeasurer : sysMon.getConfig().initialTimedScalarMeasurers) {
            tmp = initialTimedScalarMeasurer.getGroupnameOfMeasurement(key);
            if (tmp != null){
                return tmp;
            }
        }
        return "other";
    }

    private String findDescriptionForKey(String key) {
        String tmp;
        for (AScalarMeasurer initialTimedScalarMeasurer : sysMon.getConfig().initialTimedScalarMeasurers) {
            tmp = initialTimedScalarMeasurer.getDescriptionOfMeasurement(key);
            if (tmp != null){
                return tmp;
            }
        }
        return null;
    }

    private void serveData(final AJsonSerHelper json, List<String> params) throws IOException {
        final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();
        json.startObject();

        json.writeKey("timedScalars");
        json.startObject();

        for (String key : scalars.keySet()) {
            json.writeKey(key);
            json.startObject();
            addMetainfos(json, key);
            json.writeKey("selected");
            json.writeBooleanLiteral(params.contains(key));
            json.endObject();
        }

        json.endObject();
        json.endObject();
    }

    private void writeRingBufferIntoJson(final AJsonSerHelper json, final ARingBuffer buffer) throws IOException {
        for (Object aBuffer : buffer) {
            AScalarDataPoint scalarDataPoint = (AScalarDataPoint) aBuffer;
            try {
                json.startObject();

                json.writeKey("x");
                json.writeNumberLiteral(scalarDataPoint.getTimestamp(), 0);
                json.writeKey("y");
                json.writeNumberLiteral(scalarDataPoint.getValue(), scalarDataPoint.getNumFracDigits());

                json.endObject();
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    private AScalarDataPoint getAverageValue(TreeSet<AScalarDataPoint> filteredDataPoints ){
        AScalarDataPoint latestDataPoint = filteredDataPoints.last(); //TODO FOX088S check if last is ok or if it has to be first

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

    private AScalarDataPoint getAverageValue2(TreeSet<AScalarDataPoint> filteredDataPoints ){
        AScalarDataPoint latestDataPoint = filteredDataPoints.last(); //TODO FOX088S check if last is ok or if it has to be first

        final long[] sum = {0L};
        final long[] cnt = {0};
        filteredDataPoints.parallelStream().forEach(aScalarDataPoint -> {
            sum[0] += aScalarDataPoint.getValue();
            cnt[0]++;
        });

        sum[0] = (long) (sum[0] * Math.pow(10, latestDataPoint.getNumFracDigits()));
        long value = sum[0] / cnt[0];
        return new AScalarDataPoint(latestDataPoint.getTimestamp(), latestDataPoint.getName(), value, latestDataPoint.getNumFracDigits());
    }

    private TreeSet<AScalarDataPoint> filterByTimestamp(final ARingBuffer<AScalarDataPoint> buffer, Duration newerThan){
        Set<AScalarDataPoint> tmpRc = new TreeSet<>(new AScalarDataPointTimestampComparator());
        TreeSet<AScalarDataPoint> rc = new TreeSet<>(new AScalarDataPointTimestampComparator());

        final LocalDateTime localDateTime = LocalDateTime.now();
        for (AScalarDataPoint scalarDataPoint : buffer) {

            long timeStamp = scalarDataPoint.getTimestamp();
            long latestMillis = localDateTime
                    .minus(newerThan)
                    .toEpochSecond(ZoneOffset.systemDefault().getRules().getOffset(localDateTime))
                    *1000;

            if (latestMillis >= timeStamp) {
                //entry too old
                continue;
            }
            tmpRc.add(scalarDataPoint);
        }
        tmpRc.forEach(rc::add);
        return rc;
    }

    private void writeRingBufferIntoJson(final AJsonSerHelper json, final ARingBuffer<AScalarDataPoint> buffer, Duration newerThan) throws IOException {

        for (AScalarDataPoint scalarDataPoint : filterByTimestamp(buffer, newerThan)) {
            try {
                long timeStamp = scalarDataPoint.getTimestamp();

                json.startObject();

                json.writeKey("x");
                json.writeNumberLiteral(timeStamp, 0);
                json.writeKey("y");
                json.writeNumberLiteral(scalarDataPoint.getValue(), scalarDataPoint.getNumFracDigits());

                json.endObject();
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    @Override
    public void getDataForExport(OutputStream os) throws IOException {
        final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();
        List<String> params = new ArrayList<>();
        for (String key : scalars.keySet()) {
            params.add(key);
        }
        String selectedEntries = params.stream().collect(Collectors.joining(","));

        AJsonSerHelper aJsonSerHelper = new AJsonSerHelper(os);
        serveGraphData(aJsonSerHelper, Collections.singletonList(selectedEntries));

    }

}
