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

    //TODO FOX088S http://localhost:8181/nsysmon/_$_nsysmon_$_/rest/overviewDebuggingData/getData

    @Override
    public String getId() {
        return "overviewDebuggingData";
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
        return "CtrlOverviewDebugging";
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getData".equals(service)) {
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

        addPageDefinitions(json);
        addConfiguration(json);

        json.endObject();
    }

    private void addConfiguration(final AJsonSerHelper json) throws IOException {
        json.writeKey("configurationParameters");
        json.startArray();

        json.startObject();
        json.writeKey("key");
        json.writeStringLiteral("durationOfOneTimedScalar");
        json.writeKey("value");
        json.writeNumberLiteral(sysMon.getConfig().durationOfOneTimedScalar, 0);
        json.endObject();

        json.startObject();
        json.writeKey("key");
        json.writeStringLiteral("collectSqlParameters");
        json.writeKey("value");
        json.writeBooleanLiteral(sysMon.getConfig().collectSqlParameters);
        json.endObject();

        json.endArray();
    }

    private void addPageDefinitions(final AJsonSerHelper json) throws IOException {
        final Map<String, APresentationPageDefinition> pageDefs = new ConcurrentHashMap<>();
        for (APresentationMenuEntry menuEntry : sysMon.getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                pageDefs.put(pageDef.getId(), pageDef);
            }
        }

        json.writeKey("pageDefinitions");
        json.startArray();

        for (APresentationPageDefinition value : pageDefs.values()) {
            //LOG.info("processing" + stringAPresentationPageDefinitionEntry.getKey());
            json.startObject();

            json.writeKey("id");
            json.writeStringLiteral(value.getId());

            json.writeKey("fullLabel");
            json.writeStringLiteral(value.getFullLabel());

            json.endObject();
        }
        json.endArray();
    }
}
