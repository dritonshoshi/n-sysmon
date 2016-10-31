package com.nsysmon.servlet.timedscalars;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.impl.RobustScalarMeasurerWrapper;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.measure.scalar.ScalarMeasurerStatus;
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
            serveData(json, params, sysMon);
            return true;
        }else if ("getGraphData".equals(service)) {
            serveGraphData(json, params);
            return true;
        }else if ("getLatestGraphData".equals(service)) {
            serveLatestGraphData(json, params);
            return true;
        }else if ("restartTimedScalar".equals(service)) {
            restartTimedScalar(json, params, sysMon);
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
                    addMetainfos(json, key, sysMon);

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
                    addMetainfos(json, key, sysMon);

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

    protected void addMetainfos(AJsonSerHelper json, String key, NSysMonApi givenSysMon) throws IOException {
        json.writeKey("key");
        json.writeStringLiteral(key);

        String description = findDescriptionForKey(key, givenSysMon);
        if (description != null) {
            json.writeKey("description");
            json.writeStringLiteral(description);
        }

        String group = findGroupForKey(key, givenSysMon);
        if (group != null) {
            json.writeKey("group");
            json.writeStringLiteral(group);
        }

        //TODO FOX088S add status-text
        String status = findStatusForKey(key, givenSysMon).getStatus().name();
        if (group != null) {
            json.writeKey("status");
            json.writeStringLiteral(status);
        }
    }

    private String findGroupForKey(String key, NSysMonApi givenSysMon) {
        String tmp;
        for (AScalarMeasurer initialTimedScalarMeasurer : givenSysMon.getConfig().initialTimedScalarMeasurers) {
            tmp = initialTimedScalarMeasurer.getGroupnameOfMeasurement(key);
            if (tmp != null){
                return tmp;
            }
        }
        return "other";
    }

    private ScalarMeasurerStatus findStatusForKey(String key, NSysMonApi givenSysMon) {
        for (RobustScalarMeasurerWrapper robustScalarMeasurerWrapper : givenSysMon.getTimedScalarForDirectAccess()) {
            ScalarMeasurerStatus status = robustScalarMeasurerWrapper.getStatus(key);
            if (status.getStatus() != ScalarMeasurerStatus.Status.UNKNOWN) {
                return status;
            }
        }
        return new ScalarMeasurerStatus(ScalarMeasurerStatus.Status.UNKNOWN);
    }

    private String findDescriptionForKey(String key, NSysMonApi givenSysMon) {
        String tmp;
        for (AScalarMeasurer initialTimedScalarMeasurer : givenSysMon.getConfig().initialTimedScalarMeasurers) {
            tmp = initialTimedScalarMeasurer.getDescriptionOfMeasurement(key);
            if (tmp != null){
                return tmp;
            }
        }
        return null;
    }

    protected void serveData(final AJsonSerHelper json, List<String> params, NSysMonApi givenSysMon) throws IOException {
        final Map<String, ARingBuffer<AScalarDataPoint>> scalars = givenSysMon.getTimedScalarMeasurements();
        json.startObject();

        json.writeKey("timedScalars");
        json.startObject();

        for (String key : scalars.keySet()) {
            json.writeKey(key);
            json.startObject();
            addMetainfos(json, key, givenSysMon);
            json.writeKey("selected");
            json.writeBooleanLiteral(params.contains(key));
            json.endObject();
        }

        json.endObject();
        json.endObject();
    }

    protected void restartTimedScalar(final AJsonSerHelper json, List<String> params, NSysMonApi givenSysMon) throws IOException {
        String key = params.get(0);
        for (RobustScalarMeasurerWrapper measurerWrapper : givenSysMon.getTimedScalarForDirectAccess()) {
            if (measurerWrapper.restartIfResponsible(key)) {
                break;
            }
        }
        serveData(json, params, givenSysMon);
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
