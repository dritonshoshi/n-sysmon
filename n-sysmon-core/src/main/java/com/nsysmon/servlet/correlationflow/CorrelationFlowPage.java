package com.nsysmon.servlet.correlationflow;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.impl.NSysMonConfigurer;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        for (Map.Entry<String, List<String>> entry : dataSink.data.entrySet()) {
            if ((entry.getValue() == null || entry.getValue().isEmpty())){
                //only root-nodes
                continue;
            }
            json.startObject();
            json.writeKey("text");
            json.writeStringLiteral("Text for " + entry.getKey());//TODO FOX088S use real text
            addNumberOfChildren(json, entry.getValue());

            writeChildren(json, entry.getValue());

            json.endObject();
        }
        json.endArray();
        json.endObject();
    }

    private void writeChildren(AJsonSerHelper json, List<String> children) throws IOException {
        if (children == null || children.isEmpty()){
            //process only filled-elements
            return;
        }
        json.writeKey("nodes");
        json.startArray();
        for (String child : children) {
            json.startObject();
            json.writeKey("text");
            json.writeStringLiteral("Text for " + child);//TODO FOX088S use real text
            addNumberOfChildren(json, dataSink.data.get(child));
            writeChildren(json, dataSink.data.get(child));
            json.endObject();
        }
        json.endArray();
    }

    private void addNumberOfChildren(AJsonSerHelper json, List<String> children) throws IOException {
        if (children == null || children.isEmpty()){
            return;
        }
        json.writeKey("tags");
        json.startArray();

        int cnt = 0;
        for (String child : children) {
            cnt += 1;
            cnt += countChildren(child);
        }
        json.writeStringLiteral(String.valueOf(cnt));

        json.endArray();

    }

    private int countChildren(String child) {
        int cnt = 0;
        List<String> children = dataSink.data.get(child);
        if (children != null){
            cnt += children.size();
            for (String subChild : children) {
                cnt += countChildren(subChild);
            }
        }
        return cnt;
    }
}
