package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.util.DaemonThreadFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataFileGenerator implements APresentationPageDefinition {

    @Override public String getId() {
        return "dataFileGenerator";
    }

    @Override public String getShortLabel() {
        return "File Generator";
    }

    @Override public String getFullLabel() {
        return "Generate Data Files on Server";
    }

    @Override public String getHtmlFileName() {
        return "datafilegenerator.html";
    }

    @Override public String getControllerName() {
        return "CtrlDataFileGenerator";
    }

    private List<String> pagesToStore = new ArrayList<>();
    private ScheduledExecutorService scheduledPool;
    private int minutesToWait = 30; //TODO FOX088S move this to config
    private final DataFileGeneratorThread thread;
    private final Path outputPath = Paths.get("/tmp");//TODO FOX088S move this to config

    public DataFileGenerator(String pagesToStoreAsString) {
        for (String pageClassName : pagesToStoreAsString.trim().split(",")) {
            pagesToStore.add(pageClassName.trim());
        }

        scheduledPool = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
        thread = new DataFileGeneratorThread(pagesToStore, outputPath);
        scheduledPool.scheduleAtFixedRate(thread, 0, minutesToWait, TimeUnit.SECONDS);//TODO FOX088S change this to minutes

    }

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getData".equals(service)) {
            serveData(params, json);
            return true;
        }
        return false;
    }

    private void serveData(List<String> params, AJsonSerHelper json) throws IOException {
        //TODO List files which can be generated and timestamp when last generation was
        //TODO list configured paths + filenamepatterns
        json.startObject();
        json.writeKey("lastExportTimestamp");
        json.writeStringLiteral(thread.getLastExportTimestamp());
        json.writeKey("pages");

        json.startArray();
        for (APresentationMenuEntry menuEntry : NSysMon.get().getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                for (String pageClassName : pagesToStore) {
                    if (pageDef.getClass().getCanonicalName().equals(pageClassName)) {
                        addDataToJson(pageDef, json);
                    }
                }
            }
        }
        json.endArray();

        json.endObject();
    }

    private void addDataToJson(APresentationPageDefinition pageDef, AJsonSerHelper json) throws IOException {
        json.startObject();
        json.writeKey("id");
        json.writeStringLiteral(pageDef.getId());
        json.writeKey("fullLabel");
        json.writeStringLiteral(pageDef.getFullLabel());
        json.endObject();
    }

    @Override public void init(NSysMonApi sysMon) {

    }
}
