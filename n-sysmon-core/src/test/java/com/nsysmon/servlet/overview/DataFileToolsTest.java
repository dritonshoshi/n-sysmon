package com.nsysmon.servlet.overview;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class DataFileToolsTest {

    @Test public void getNsysmonControllerIdFromFilename(){
        //Dyyyymmdd.Thhmmss
        //D20160309.T17055
        String fileName = "Part1Part/nsysmon_installation_host_Part2Part_D20160309.T170552";
        String id = new DataFileTools().getNsysmonControllerIdFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("Part2Part", id);
    }

    @Test public void getNsysmonControllerIdFromFilename_WithoutDirectory(){
        String fileName = "nsysmon_installation_host_overviewDebuggingData_D20160309.T170552";
        String id = new DataFileTools().getNsysmonControllerIdFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("overviewDebuggingData", id);
    }

    @Test public void toFilename(){
        String outputPath ="Part1Part";
        String pageDef = "Part2Part";
        String hostName = "hostName";
        String installation = "installation";
        LocalDateTime fileDate = LocalDateTime.now();
        String fileName = new DataFileTools().toGzipFilename(outputPath, pageDef, fileDate, hostName, installation);

        Assert.assertNotNull(fileName);
        Assert.assertTrue(fileName.contains(outputPath));
        Assert.assertTrue(fileName.contains(pageDef));
        Assert.assertTrue(fileName.contains(hostName));
        Assert.assertTrue(fileName.contains(installation));
    }

    @Test public void gethostNameFromFilename(){
        String fileName = "nsysmon_installation_host_overviewDebuggingData_D20160309.T170552";
        String id = new DataFileTools().getHostFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("host", id);
    }

    @Test public void getDateFromFilename(){
        String fileName = "nsysmon_installation_host_overviewDebuggingData_D20160309.T170552";
        String id = new DataFileTools().getDateFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("2016-03-09", id);
    }

    @Test public void getTimeFromFilename(){
        String fileName = "nsysmon_installation_host_overviewDebuggingData_D20160309.T170551";
        String id = new DataFileTools().getTimeFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("17:05:51", id);
    }

    @Test public void getinstallationFromFilename(){
        String fileName = "nsysmon_installation_host_overviewDebuggingData_D20160309.T170552";
        String id = new DataFileTools().getInstallationFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("installation", id);
    }

}
