package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OverviewDebuggingData implements APresentationPageDefinition {
    private volatile NSysMonApi sysMon;
    private static final NSysMonLogger LOG = NSysMonLogger.get(OverviewDebuggingData.class);

    @Override
    public String getId() {
        return "overviewDebugData";
    }

    @Override
    public String getShortLabel() {
        return "Debugging";
    }

    @Override
    public String getFullLabel() {
        return "Debugging Data";
    }

    @Override
    public String getHtmlFileName() {
        return "overviewdebugging.html";
    }

    @Override
    public String getControllerName() {
        return "CtrlOverviewDebug";
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getData".equals(service)) {
            //TODO FOX088S
            serveData(params, json);
            return true;
        }
        return false;
    }

    @Override
    public void init(NSysMonApi sysMon) {
        this.sysMon = sysMon;

    }

    private void serveData(final List<String> params, final AJsonSerHelper json) throws IOException {
        json.startObject();
        json.writeKey(getId());

        addPageDefinitions(json);
        addConfiguration(json);

        json.endObject();
    }

    private void addConfiguration(final AJsonSerHelper json) throws IOException {
        json.writeKey("configurationParameters");
        json.startObject();

        json.writeKey("durationOfOneTimedScalar");
        json.writeNumberLiteral(sysMon.getConfig().durationOfOneTimedScalar, 0);

        json.writeKey("collectSqlParameters");
        json.writeBooleanLiteral(sysMon.getConfig().collectSqlParameters);
        json.endObject();
    }

    private void addPageDefinitions(final AJsonSerHelper json) throws IOException {
        final Map<String, APresentationPageDefinition> pageDefs = new ConcurrentHashMap<>();
        for (APresentationMenuEntry menuEntry : sysMon.getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                pageDefs.put(pageDef.getId(), pageDef);
            }
        }

        json.startObject();
        json.writeKey("pageDefinitions");
        json.startArray();

        for (Map.Entry<String, APresentationPageDefinition> stringAPresentationPageDefinitionEntry : pageDefs.entrySet()) {
            LOG.info("processing" + stringAPresentationPageDefinitionEntry.getKey());
            json.startObject();

            json.writeKey("id");
            json.writeStringLiteral(stringAPresentationPageDefinitionEntry.getKey());

            json.writeKey("fullLabel");
            json.writeStringLiteral(stringAPresentationPageDefinitionEntry.getValue().getFullLabel());

            json.endObject();
        }
        json.endArray();
        json.endObject();
    }
}
