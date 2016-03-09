package com.nsysmon.servlet.overview;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DataFileTools {
    public String toFilename(String outputPath, String pageDef, LocalDateTime fileDate) {
        //TODO FOX088S
        String rc = outputPath + "/" + DataFileGeneratorSupporter.DATAFILE_PREFIX + pageDef + "_" + fileDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replaceAll(":","_");
        return rc;
    }

    public String getNsysmonControllerIdFromFilename(String filename) {
        //TODO FOX088S add not-found-checks
        int lastIndexOf = filename.lastIndexOf('/');
        String substring;
        if (lastIndexOf != -1){
            substring = filename.substring(lastIndexOf);
        }else{
            substring = filename;
        }

        int index1 = substring.indexOf('_');
        int index2 = substring.indexOf('_', index1 + 1);
        String rc = substring.substring(index1 + 1, index2);
        return rc;
    }
}
