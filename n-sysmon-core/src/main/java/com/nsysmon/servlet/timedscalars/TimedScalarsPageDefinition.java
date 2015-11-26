package com.nsysmon.servlet.timedscalars;

import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.AScalarDataPoint;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;



public class TimedScalarsPageDefinition implements APresentationPageDefinition {
    private volatile NSysMonApi sysMon;

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
            serveData(json);
            return true;
        }else if ("getGraphData".equals(service)) {
            serveGraphData(json, params);
            return true;
        }

        return false;
    }

    private void serveGraphData(final AJsonSerHelper json, List<String> params) throws IOException {
        //TODO FOX088S why is this called twice at start?
        for (String param : params) {
            String paramWithoutHtml = URLDecoder.decode(param, "UTF-8");
            final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();

            json.startArray();
            for (String key : scalars.keySet()) {
                if (key.equalsIgnoreCase(paramWithoutHtml)) {
                    json.startObject();
                    json.writeKey("key");
                    json.writeStringLiteral(key);
                    json.writeKey("values");
                    json.startArray();
                    writeRingBuffer(json, scalars.get(key));
                    json.endArray();
                    json.endObject();
                }
            }
            json.endArray();

        }

    }

    //TODO FOX088S think about splitting this into 2 methods one for the names and one for the data
    private void serveData(final AJsonSerHelper json) throws IOException {
        final Map<String, ARingBuffer<AScalarDataPoint>> scalars = sysMon.getTimedScalarMeasurements();
        json.startObject();

        json.writeKey("timedScalars");
        json.startObject();

        for (String key : scalars.keySet()) {
            json.writeKey(key);
            json.startObject();
            json.writeKey("key");
            json.writeStringLiteral(key);
            json.endObject();
        }

        json.endObject();
        json.endObject();
    }

    private void writeRingBuffer(final AJsonSerHelper json, final ARingBuffer buffer) throws IOException {
        Iterator iterator = buffer.iterator();
while (iterator.hasNext()){
    AScalarDataPoint scalarDataPoint = (AScalarDataPoint)iterator.next();
    try {
        json.startObject();

        json.writeKey("x");
        json.writeNumberLiteral(scalarDataPoint.getTimestamp(), 0);
        json.writeKey("y");
        json.writeNumberLiteral(scalarDataPoint.getValue(), scalarDataPoint.getNumFracDigits());

        json.endObject();
    } catch (IOException e) {
        System.err.println(e);
    }
}

/*
        buffer.forEach(new Consumer() {
            @Override
            public void accept(Object o) {
                try {
                    AScalarDataPoint scalarDataPoint = (AScalarDataPoint) o;
                    json.startObject();

                    json.writeKey("x");
                    json.writeNumberLiteral(scalarDataPoint.getTimestamp(), 0);
                    json.writeKey("y");
                    json.writeNumberLiteral(scalarDataPoint.getValue(), scalarDataPoint.getNumFracDigits());

                    json.endObject();
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
        });
        */
    }
}
