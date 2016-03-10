package com.nsysmon.servlet.overview;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class DataFileToolsTest {

    @Test public void getNsysmonControllerIdFromFilename(){
        String fileName = "Part1Part/nsysmon_server_market_Part2Part_2016-03-09T19:01:55.178";
        String id = new DataFileTools().getNsysmonControllerIdFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("Part2Part", id);
    }

    @Test public void getNsysmonControllerIdFromFilename_WithoutDirectory(){
        String fileName = "nsysmon_server_market_overviewDebuggingData_2016-03-09T20_58_03.173";
        String id = new DataFileTools().getNsysmonControllerIdFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("overviewDebuggingData", id);
    }

    @Test public void toFilename(){
        String outputPath ="Part1Part";
        String pageDef = "Part2Part";
        String serverName = "serverName";
        String market = "market";
        LocalDateTime fileDate = LocalDateTime.now();
        String fileName = new DataFileTools().toFilename(outputPath, pageDef, fileDate, serverName, market);

        Assert.assertNotNull(fileName);
        Assert.assertTrue(fileName.contains(outputPath));
        Assert.assertTrue(fileName.contains(pageDef));
        Assert.assertTrue(fileName.contains(serverName));
        Assert.assertTrue(fileName.contains(market));
    }

}
