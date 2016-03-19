package com.nsysmon.servlet.overview;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("WeakerAccess")
class DataFileTools {
    private final static DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final static DateTimeFormatter time = DateTimeFormatter.ofPattern("HHmmss");

    @SuppressWarnings("StringBufferReplaceableByString")
    public String toGzipFilename(String outputPath, String pageDef, LocalDateTime fileDate, String host, String installation) {
        StringBuilder rc = new StringBuilder();

        rc.append(outputPath);
        rc.append("/");
        rc.append(DataFileGeneratorSupporter.DATAFILE_PREFIX);
        rc.append("_");
        rc.append(installation);
        rc.append("_");
        rc.append(host);
        rc.append("_");
        rc.append(pageDef);
        rc.append("_");
        rc.append(getTimeStamp(fileDate));
        rc.append(".gz");

        return rc.toString();
    }

    private static String getTimeStamp(LocalDateTime dateTime) {
        return "D" + date.format(dateTime) + ".T" + time.format(dateTime);
    }

    public String getNsysmonControllerIdFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);

        int index1 = substring.indexOf('_');
        if (index1 == -1){
            return "<unknown>";
        }
        int index2 = substring.indexOf('_', index1 + 1);
        if (index2 == -1){
            return "<unknown>";
        }
        int index3 = substring.indexOf('_', index2 + 1);
        if (index3 == -1){
            return "<unknown>";
        }
        int index4 = substring.indexOf('_', index3 + 1);
        if (index4 == -1){
            return "<unknown>";
        }
        return substring.substring(index3 + 1, index4);
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
        String tmp = substring.split("_")[4].substring(1,9);
        if (tmp.length() < 8){
            return "<unknown>";
        }
        return tmp.substring(0,4) + "-" + tmp.substring(4,6) + "-" + tmp.substring(6,8);
    }

    public String getTimeFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        String tmp = substring.split("_")[4].substring(11);
        if (tmp.length() < 6){
            return "<unknown>";
        }
        return tmp.substring(0,2) + ":" + tmp.substring(2,4) + ":" + tmp.substring(4,6);
    }

    public String getInstallationFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        return substring.split("_")[1];
    }

    public String getHostFromFilename(String filename) {
        String substring = getFilenameWithoutDirectories(filename);
        return substring.split("_")[2];
    }
}
