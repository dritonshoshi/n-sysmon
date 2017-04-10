package com.nsysmon.servlet.memgc;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.servlet.overview.DataFileGeneratorSupporter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;


/**
 * @author arno
 */
public class AMemGcPageDefinition implements APresentationPageDefinition, DataFileGeneratorSupporter {
    private final int bufferSize;

    public AMemGcPageDefinition(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    private volatile GcDataSink gcDataSink;

    @Override public String getId() {
        return "memgc";
    }

    @Override public String getShortLabel() {
        return "GC View";
    }

    @Override public String getFullLabel() {
        return "Garbage Collection History";
    }

    @Override public String getHtmlFileName() {
        return "memgc.html";
    }

    @Override public String getControllerName() {
        return "CtrlMemGc";
    }

    @Override public void init(NSysMonApi sysMon) {
        gcDataSink = new GcDataSink(bufferSize);
        NSysMonConfigurer.addDataSink(sysMon, gcDataSink);
    }

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) throws IOException {
        if("getData".equals(service)) {
            serveData(json);
            return true;
        }

        return false;
    }

    private void serveData(AJsonSerHelperForNSysmon json) throws IOException {
        json.startObject();

        json.writeKey("gcs");
        json.startArray();
        for(GcDetails gc: gcDataSink.getData()) {
            serveGcDetails(json, gc);
        }
        json.endArray();

        json.endObject();

    }

    private void serveGcDetails(AJsonSerHelperForNSysmon json, GcDetails gc) throws IOException {
        json.startObject();

        json.writeKey("type");
        json.writeStringLiteral(gc.gcType);

        json.writeKey("algorithm");
        json.writeStringLiteral(gc.algorithm);

        json.writeKey("cause");
        json.writeStringLiteral(gc.cause);

        json.writeKey("startMillis");
        json.writeNumberLiteral(gc.startMillis, 0);

        json.writeKey("durationNanos");
        json.writeNumberLiteral(gc.durationNanos, 0);

        json.writeKey("mem");
        serveMemDetails(json, gc.memDetails);

        json.endObject();
    }

    private void serveMemDetails(AJsonSerHelperForNSysmon json, List<GcMemDetails> memDetails) throws IOException {
        json.startObject();

        for(GcMemDetails mem: memDetails) {
            json.writeKey(mem.memKind);
            json.startObject();

            json.writeKey("usedBefore");
            json.writeNumberLiteral(mem.usedBefore, 0);

            json.writeKey("usedAfter");
            json.writeNumberLiteral(mem.usedAfter, 0);

            json.writeKey("committedBefore");
            json.writeNumberLiteral(mem.committedBefore, 0);

            json.writeKey("committedAfter");
            json.writeNumberLiteral(mem.committedAfter, 0);

            json.endObject();
        }

        json.endObject();
    }

    @Override
    public void getDataForExport(OutputStream os) throws IOException {
        AJsonSerHelperForNSysmon aJsonSerHelperForNSysmon = new AJsonSerHelperForNSysmon(os);
        serveData(aJsonSerHelperForNSysmon);
    }

    //TODO getMostRecentTimestamp
}
