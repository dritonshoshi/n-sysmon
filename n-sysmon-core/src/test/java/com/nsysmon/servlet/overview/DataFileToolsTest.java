package com.nsysmon.servlet.overview;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class DataFileToolsTest {

    @Test public void getNsysmonControllerIdFromFilename(){
        String fileName = "Part1Part/nsysmon_market_server_Part2Part_2016-03-09T19:01:55.178";
        String id = new DataFileTools().getNsysmonControllerIdFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("Part2Part", id);
    }

    @Test public void getNsysmonControllerIdFromFilename_WithoutDirectory(){
        String fileName = "nsysmon_market_server_overviewDebuggingData_2016-03-09T20_58_03.173";
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
        String fileName = new DataFileTools().toGzipFilename(outputPath, pageDef, fileDate, serverName, market);

        Assert.assertNotNull(fileName);
        Assert.assertTrue(fileName.contains(outputPath));
        Assert.assertTrue(fileName.contains(pageDef));
        Assert.assertTrue(fileName.contains(serverName));
        Assert.assertTrue(fileName.contains(market));
    }

    @Test public void getServerNameFromFilename(){
        String fileName = "nsysmon_market_server_overviewDebuggingData_2016-03-09T20_58_03.173";
        String id = new DataFileTools().getServerNameFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("server", id);
    }

    @Test public void getDateFromFilename(){
        String fileName = "nsysmon_market_server_overviewDebuggingData_2016-03-09T20_58_03.173";
        String id = new DataFileTools().getDateFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("2016-03-09", id);
    }

    @Test public void getTimeFromFilename(){
        String fileName = "nsysmon_market_server_overviewDebuggingData_2016-03-09T20_58_03.173";
        String id = new DataFileTools().getTimeFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("20:58:03", id);
    }

    @Test public void getMarketFromFilename(){
        String fileName = "nsysmon_market_server_overviewDebuggingData_2016-03-09T20_58_03.173";
        String id = new DataFileTools().getMarketFromFilename(fileName);
        Assert.assertNotNull(id);
        Assert.assertEquals("market", id);
    }

}
