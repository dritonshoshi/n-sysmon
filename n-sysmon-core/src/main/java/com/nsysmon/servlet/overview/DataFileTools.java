package com.nsysmon.servlet.overview;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DataFileTools {

    public String toFilename(String outputPath, String pageDef, LocalDateTime fileDate, String serverName, String market) {
        //TODO FOX088S
        String rc = outputPath + "/" + DataFileGeneratorSupporter.DATAFILE_PREFIX + "_" + market + "_" + serverName + "_" + pageDef + "_" + fileDate
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replaceAll(":", "_");
        return rc;
    }

    public String getNsysmonControllerIdFromFilename(String filename) {
        //TODO FOX088S add not-found-checks
        int lastIndexOf = filename.lastIndexOf('/');
        String substring;
        if (lastIndexOf != -1) {
            substring = filename.substring(lastIndexOf);
        }
        else {
            substring = filename;
        }

        int index1 = substring.indexOf('_');
        int index2 = substring.indexOf('_', index1 + 1);
        int index3 = substring.indexOf('_', index2 + 1);
        int index4 = substring.indexOf('_', index3 + 1);
        String rc = substring.substring(index3 + 1, index4);
        return rc;
    }
}
