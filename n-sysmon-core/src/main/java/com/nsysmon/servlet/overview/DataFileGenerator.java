package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.util.DaemonThreadFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataFileGenerator implements APresentationPageDefinition {

    private static final NSysMonLogger LOG = NSysMonLogger.get(DataFileGenerator.class);

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

    /* this datasink isn't used for data, just for receiving the shut-down-trigger when shutting down NSysmon */
    private final ADataSink dataSink = new ADataSink() {
        @Override
        public void onStartedHierarchicalMeasurement(String identifier) {
            //nothing
        }

        @Override
        public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            //nothing
        }

        @Override
        public void shutdown() throws Exception {
            LOG.info("Exporting all data one final time.");
            // export all data when nsysmon get's shutdown
            for (DataFileGeneratorThread dataFileGeneratorThread : pageStorer) {
                dataFileGeneratorThread.run();
            }
        }
    };

    private final List<DataFileGeneratorThread> pageStorer = new ArrayList<>();
    private final ScheduledExecutorService scheduledPool;

    public DataFileGenerator(String pagesToStoreAsString) {
        scheduledPool = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
        for (String pageClassName : pagesToStoreAsString.trim().split(",")) {
            String pageId = pageClassName.trim().split(":")[0];
            if (null != pageId && pageId.trim().length() != 0){
                try{
                    int minutesToWait = Integer.valueOf(pageClassName.trim().split(":")[1]);
                    DataFileGeneratorThread thread = new DataFileGeneratorThread(pageId, minutesToWait);
                    LOG.info(thread.toString());
                    pageStorer.add(thread);
                    scheduledPool.scheduleAtFixedRate(thread, 1, minutesToWait, TimeUnit.MINUTES);
                }catch (Exception e){
                    LOG.error("Export for " + pageId + " not activated. Please check your configuration.");
                }
            }
        }
    }

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) throws Exception {
        if ("getData".equals(service)) {
            serveData(params, json);
            return true;
        }
        return false;
    }

    /** Lists files which can be generated and timestamp when last generation was */
    private void serveData(List<String> params, AJsonSerHelperForNSysmon json) throws IOException {
        //TODO list configured paths + filename-patterns
        json.startObject();
        json.writeKey("pages");

        json.startArray();
        for (DataFileGeneratorThread thread : pageStorer) {
            addDataToJson(thread, json);
        }
        json.endArray();

        json.endObject();
    }

    private void addDataToJson(DataFileGeneratorThread thread, AJsonSerHelperForNSysmon json) throws IOException {
        json.startObject();
        json.writeKey("id");
        json.writeStringLiteral(thread.getPageId());
        json.writeKey("fullLabel");
        json.writeStringLiteral(thread.getPage().getFullLabel());
        json.writeKey("timeToWait");
        json.writeNumberLiteral(thread.getTimeToWait(), 0);
        json.writeKey("lastExportTimestamp");
        json.writeStringLiteral(thread.getLastExportTimestamp());
        json.endObject();
    }

    @Override public void init(NSysMonApi sysMon) {
        NSysMonConfigurer.addDataSink(sysMon, dataSink);
    }
}
