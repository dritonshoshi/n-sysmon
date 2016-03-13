package com.nsysmon.servlet.correlationflow;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.impl.NSysMonConfigurer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CorrelationFlowPage implements APresentationPageDefinition {
    private volatile NSysMonApi sysMon;
    private static final NSysMonLogger LOG = NSysMonLogger.get(CorrelationFlowPage.class);
    private final CorrelationFlowDataSink dataSink;

    public CorrelationFlowPage() {
        dataSink = new CorrelationFlowDataSink();
    }

    @Override
    public String getId() {
        return "correlationFlow";
    }

    @Override
    public String getShortLabel() {
        return "CorrelationFlow";
    }

    @Override
    public String getFullLabel() {
        return "Correlation Flow";
    }

    @Override
    public String getHtmlFileName() {
        return "correlationflow.html";
    }

    @Override
    public String getControllerName() {
        return "CtrlCorrelationFlow";
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getData".equals(service)) {
            serveData(json);
            return true;
        }
        return false;
    }

    @Override
    public void init(NSysMonApi sysMon) {
        this.sysMon = sysMon;
        NSysMonConfigurer.addDataSink(sysMon, dataSink);
    }

    //Test at http://localhost:8181/nsysmon/_$_nsysmon_$_/rest/correlationFlow/getData
    private void serveData(final AJsonSerHelper json) throws IOException {
        json.startObject();
        json.writeKey("tree");
        json.startArray();
        Map<ACorrelationId, List<ACorrelationId>> data = dataSink.getData();
        for (Map.Entry<ACorrelationId, List<ACorrelationId>> entry : data.entrySet()) {
            if (entry.getKey().getIdParent() != null){
                //only root-nodes as starting point
                continue;
            }
            json.startObject();
            json.writeKey("text");
            json.writeStringLiteral(entry.getKey().getQualifier());
            addNumberOfChildren(json, entry.getValue(), data);

            writeChildren(json, entry.getValue(), data);

            json.endObject();
        }
        json.endArray();
        json.endObject();
    }

    private void writeChildren(AJsonSerHelper json, List<ACorrelationId> children, Map<ACorrelationId, List<ACorrelationId>> data) throws IOException {
        if (children == null || children.isEmpty()){
            //process only filled-elements
            return;
        }

        json.writeKey("nodes");
        json.startArray();
        for (ACorrelationId child : children) {
            json.startObject();
            json.writeKey("text");
            json.writeStringLiteral(child.getQualifier());
            addNumberOfChildren(json, data.get(child), data);
            writeChildren(json, data.get(child), data);
            json.endObject();
        }
        json.endArray();
    }

	/**
     * All children from all levels, not only the direct children.
     */
    private void addNumberOfChildren(AJsonSerHelper json, List<ACorrelationId> children, Map<ACorrelationId, List<ACorrelationId>> data) throws IOException {
        if (children == null || children.isEmpty()){
            return;
        }
        json.writeKey("tags");
        json.startArray();

        int cnt = 0;
        for (ACorrelationId child : children) {
            cnt += 1;
            cnt += countChildren(child, data);
        }
        json.writeStringLiteral(String.valueOf(cnt));

        json.endArray();
    }

    private int countChildren(ACorrelationId child, Map<ACorrelationId, List<ACorrelationId>> data) {
        int cnt = 0;
        List<ACorrelationId> children = data.get(child);
        if (children != null){
            cnt += children.size();
            for (ACorrelationId subChild : children) {
                cnt += countChildren(subChild, data);
            }
        }
        return cnt;
    }
}
