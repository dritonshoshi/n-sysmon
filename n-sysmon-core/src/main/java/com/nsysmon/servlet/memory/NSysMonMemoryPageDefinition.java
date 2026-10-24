package com.nsysmon.servlet.memory;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.impl.NSysMonMemoryReport;

import java.io.IOException;
import java.util.List;


/**
 * Shows how much heap N-SysMon estimates it uses for storing measurements, broken down per data sink. This makes it
 *  possible to identify which measurement view (trace, longest calls, drilldown, ...) is responsible for a high memory
 *  footprint.
 *
 * @author arno
 */
public class NSysMonMemoryPageDefinition implements APresentationPageDefinition {
    private volatile NSysMonApi sysMon;

    @Override public String getId() {
        return "nsysmonMemory";
    }

    @Override public String getShortLabel() {
        return "NSysMon Memory";
    }

    @Override public String getFullLabel() {
        return "NSysMon Memory Usage";
    }

    @Override public String getHtmlFileName() {
        return "memoryusage.html";
    }

    @Override public String getControllerName() {
        return "CtrlMemoryUsage";
    }

    @Override public void init(NSysMonApi sysMon) {
        this.sysMon = sysMon;
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) throws IOException {
        if (!"getData".equals(service)) {
            return false;
        }

        serveData(json);
        return true;
    }

    private void serveData(AJsonSerHelperForNSysmon json) throws IOException {
        final NSysMonMemoryReport.Report report = new NSysMonMemoryReport(sysMon).collect();
        final Runtime runtime = Runtime.getRuntime();

        json.startObject();
        writeNumber(json, "timestamp", System.currentTimeMillis());
        writeNumber(json, "jvmUsedHeapBytes", runtime.totalMemory() - runtime.freeMemory());
        writeNumber(json, "jvmMaxHeapBytes", runtime.maxMemory());
        writeNumber(json, "estimatedTotalBytes", report.estimatedTotalBytes);

        json.writeKey("dataSinks");
        writeEntries(json, report.dataSinks);

        json.writeKey("timedScalars");
        writeEntries(json, report.timedScalars);

        json.endObject();
    }

    private void writeEntries(AJsonSerHelperForNSysmon json, List<NSysMonMemoryReport.Entry> entries) throws IOException {
        json.startArray();
        for (NSysMonMemoryReport.Entry entry : entries) {
            writeEntry(json, entry);
        }
        json.endArray();
    }

    private void writeEntry(AJsonSerHelperForNSysmon json, NSysMonMemoryReport.Entry entry) throws IOException {
        json.startObject();

        json.writeKey("name");
        json.writeStringLiteral(entry.name);
        json.writeKey("className");
        json.writeStringLiteral(entry.className);
        json.writeKey("kind");
        json.writeStringLiteral(entry.kind.name());
        json.writeKey("kindLabel");
        json.writeStringLiteral(entry.kind.label);
        json.writeKey("numEntries");
        json.writeNumberLiteral(entry.numEntries, 0);
        json.writeKey("estimatedBytes");
        json.writeNumberLiteral(entry.estimatedBytes, 0);
        json.writeKey("reporting");
        json.writeBooleanLiteral(entry.reporting);

        json.endObject();
    }

    private void writeNumber(AJsonSerHelperForNSysmon json, String key, long value) throws IOException {
        json.writeKey(key);
        json.writeNumberLiteral(value, 0);
    }
}
