package com.nsysmon.servlet.overview;

import com.nsysmon.NSysMon;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public class DataFileGeneratorThread implements Runnable {
    //TODO FOX088S use YYYYMMDD instead of DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private static final NSysMonLogger LOG = NSysMonLogger.get(DataFileGeneratorThread.class);
    private final String pageId;
    private LocalDateTime lastExport;
    private final int timeToWait;
    private APresentationPageDefinition page = null;

    public DataFileGeneratorThread(String pageId, int timeToWait) {
        this.pageId = pageId;
        this.timeToWait = timeToWait;
    }

    @Override
    public void run() {
        // this cannot be done in the constructor, because the NSysMonConfig isn't initialized at that moment
        for (APresentationMenuEntry menuEntry : NSysMon.get().getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                if (pageDef.getId().equalsIgnoreCase(pageId)) {
                    this.page = pageDef;
                }
            }
        }

        lastExport = LocalDateTime.now();
        exportDataAsFile(page);
    }

    private void exportDataAsFile(APresentationPageDefinition pageDef) {
        if (!(pageDef instanceof DataFileGeneratorSupporter)) {
            LOG.error(pageDef.getId() + " is configured to be exported, but is hasn't the abilities to do so. Ignoring this entry.");
            return;
        }

        try {
            String serverName = InetAddress.getLocalHost().getHostName();
            String market = "TODO";//TODO FOX088S fill this
            String filename = new DataFileTools().toGzipFilename(NSysMon.get().getConfig().pathDatafiles, pageDef.getId(), lastExport, serverName, market);
            LOG.info("exporting to " + filename);
            FileOutputStream fos = new FileOutputStream(filename);
            GZIPOutputStream gzipOut = new GZIPOutputStream(fos);
            ((DataFileGeneratorSupporter) pageDef).getDataForExport(gzipOut);
            gzipOut.flush();
            fos.flush();
            gzipOut.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastExportTimestamp() {
        return lastExport.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public int getTimeToWait() {
        return timeToWait;
    }

    public String getPageId() {
        return pageId;
    }

    public APresentationPageDefinition getPage() {
        return page;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataFileGeneratorThread[");
        sb.append("page=");
        sb.append(pageId);
        sb.append(" ,");
        sb.append("timeToWait=");
        sb.append(timeToWait);
        sb.append(" ,");
        sb.append("lastExport=");
        sb.append(lastExport);
        sb.append(",");
        sb.append("]");
        return sb.toString();
    }
}
