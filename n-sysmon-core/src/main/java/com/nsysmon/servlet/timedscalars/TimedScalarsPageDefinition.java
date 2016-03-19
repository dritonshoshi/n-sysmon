package com.nsysmon.servlet.timedscalars;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.servlet.overview.DataFileGeneratorSupporter;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
                    json.writeKey("key");
                    json.writeStringLiteral(key);
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

    private void serveData(final AJsonSerHelper json, List<String> params) throws IOException {
        final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();
        json.startObject();

        json.writeKey("timedScalars");
        json.startObject();

        for (String key : scalars.keySet()) {
            json.writeKey(key);
            json.startObject();
            json.writeKey("key");
            json.writeStringLiteral(key);
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
