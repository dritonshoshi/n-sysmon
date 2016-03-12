package com.nsysmon.servlet.overview;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DataFileTools {

    public String toGzipFilename(String outputPath, String pageDef, LocalDateTime fileDate, String serverName, String market) {
        StringBuilder rc = new StringBuilder();

        rc.append(outputPath);
        rc.append("/");
        rc.append(DataFileGeneratorSupporter.DATAFILE_PREFIX);
        rc.append("_");
        rc.append(market);
        rc.append("_");
        rc.append(serverName);
        rc.append("_");
        rc.append(pageDef);
        rc.append("_");
        rc.append(fileDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replaceAll(":", "_"));
        rc.append(".gz");

        return rc.toString();
    }

    public String getNsysmonControllerIdFromFilename(String filename) {
        //TODO FOX088S add not-found-checks
        String substring = getFilenameWithoutDirectories(filename);

        int index1 = substring.indexOf('_');
        int index2 = substring.indexOf('_', index1 + 1);
        int index3 = substring.indexOf('_', index2 + 1);
        int index4 = substring.indexOf('_', index3 + 1);
        String rc = substring.substring(index3 + 1, index4);
        return rc;
    }

    private String getFilenameWithoutDirectories(String filename) {
        String substring;
        int lastIndexOf = filename.lastIndexOf('/');
        if (lastIndexOf != -1) {
            substring = filename.substring(lastIndexOf);
        } else {
            substring = filename;
        }
        return substring;
    }

    public String getDateFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        String rc = substring.split("_")[4].substring(0,10);
        return rc;
    }

    public String getTimeFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        String[] split = substring.split("_");
        String rc = split[4].substring(11) + ":" + split[5] + ":" + split[6].substring(0,2);
        return rc;
    }

    public String getMarketFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        String rc = substring.split("_")[1];
        return rc;
    }

    public String getServerNameFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        String rc = substring.split("_")[2];
        return rc;
    }
}
