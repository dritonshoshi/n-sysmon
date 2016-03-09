package com.nsysmon.servlet.overview;

import com.nsysmon.NSysMon;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataFileGeneratorThread implements Runnable {
    private static final NSysMonLogger LOG = NSysMonLogger.get(DataFileGeneratorThread.class);
    private final List<String> pagesToStore;
    private final Path outputPath;
    private LocalDateTime lastExport;

    public DataFileGeneratorThread(List<String> pagesToStore, Path outputPath) {
        this.pagesToStore = pagesToStore;
        this.outputPath = outputPath;
    }

    @Override
    public void run() {
        lastExport = LocalDateTime.now();
        for (APresentationMenuEntry menuEntry : NSysMon.get().getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                for (String pageIdToStore : pagesToStore) {
                    if (pageDef.getId().equalsIgnoreCase(pageIdToStore)) {
                        exportDataAsFile(pageDef);
                    }
                }
            }
        }
    }

    private void exportDataAsFile(APresentationPageDefinition pageDef) {
        if (!(pageDef instanceof DataFileGeneratorSupporter)) {
            LOG.error(pageDef.getId() + " is configured to be exported, but is hasn't the abilities to do so. Ignoring this entry.");
            return;
        }

        try {
            String filename = new DataFileTools().toFilename(outputPath.toString(), pageDef.getId(), lastExport);
            LOG.info("exporting to " + filename);
            FileOutputStream fos = new FileOutputStream(filename);
            ((DataFileGeneratorSupporter) pageDef).getDataForExport(fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastExportTimestamp() {
        return lastExport.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
