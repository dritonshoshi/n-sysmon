package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
// http://localhost:8181/nsysmon/_$_nsysmon_$_/rest/loadableServerDataFiles/getFiles
public class LoadableServerDataFiles implements APresentationPageDefinition {

    @Override
    public String getId() {
        return "loadableServerDataFiles";
    }

    @Override
    public String getShortLabel() {
        return "Datafiles";
    }

    @Override
    public String getFullLabel() {
        return "List Data Files on Server";
    }

    @Override
    public String getHtmlFileName() {
        return "loadableserverdatafiles.html";
    }

    @Override
    public String getControllerName() {
        return "CtrlLoadableServerDataFiles";
    }

    @Override
    public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getFiles".equals(service)) {
            getFilesAsJson(params, json);
            return true;
        } else if ("loadFromFile".equals(service)) {
            loadFile(params, json);
            return true;
        }
        return false;
    }

    private void loadFile(List<String> params, AJsonSerHelper json) throws IOException {
        //TODO FOX088S error-handling
        //TODO FOX088S security, check params.get(0)

        Path path = Paths.get(Paths.get(NSysMon.get().getConfig().pathDatafiles).toString(), params.get(0));
        FileInputStream fis = new FileInputStream(path.toFile());
        GZIPInputStream gzipIn = new GZIPInputStream(fis);

        copyData(new InputStreamReader(gzipIn), json.getOut());

        json.getOut().flush();
        gzipIn.close();
        fis.close();
    }

    private void copyData(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1024];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    private void getFilesAsJson(List<String> params, AJsonSerHelper json) throws IOException {
        DataFileTools dataTools = new DataFileTools();

        //temporary remember the names, so they can be used in the display
        Map<String, String> controllerid2controllerName = new HashMap<>();
        for (APresentationMenuEntry menuEntry : NSysMon.get().getConfig().presentationMenuEntries) {
            for (APresentationPageDefinition pageDef : menuEntry.pageDefinitions) {
                controllerid2controllerName.put(pageDef.getId(), pageDef.getFullLabel());
            }
        }


        json.startObject();
        json.writeKey("files");
        json.startArray();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(NSysMon.get().getConfig().pathDatafiles))) {
            for (Path path : directoryStream) {
                if ((!path.getFileName().toString().startsWith(DataFileGeneratorSupporter.DATAFILE_PREFIX))||(!path.getFileName().toString().endsWith(".gz"))) {
                    continue;
                }
                String host = dataTools.getHostFromFilename(path.getFileName().toString());
                String installation = dataTools.getInstallationFromFilename(path.getFileName().toString());
                String dateAsString = dataTools.getDateFromFilename(path.getFileName().toString());
                String controllerId = dataTools.getNsysmonControllerIdFromFilename(path.getFileName().toString());

                json.startObject();
                fillFileDataAsJson(json, dataTools, path, host, installation, dateAsString, controllerId, controllerid2controllerName.get(controllerId));
                json.endObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        json.endArray();

        json.endObject();

    }

    private void fillFileDataAsJson(AJsonSerHelper json, DataFileTools dataTools, Path path, String host, String installation, String dateAsString, String controllerId, String controllerName) throws IOException {
        json.writeKey("name");
        json.writeStringLiteral(path.getFileName().toString());

        String pageId = new DataFileTools().getNsysmonControllerIdFromFilename(path.toString());
        json.writeKey("page");
        json.writeStringLiteral(pageId);

        json.writeKey("processor");
        json.writeStringLiteral(dataTools.getNsysmonControllerIdFromFilename(path.getFileName().toString()));

        json.writeKey("size");
        json.writeNumberLiteral(Files.size(path), 0);

        json.writeKey("date");
        json.writeStringLiteral(dateAsString);

        json.writeKey("time");
        json.writeStringLiteral(dataTools.getTimeFromFilename(path.getFileName().toString()));

        json.writeKey("installation");
        json.writeStringLiteral(installation);

        json.writeKey("host");
        json.writeStringLiteral(host);

        json.writeKey("controllerId");
        json.writeStringLiteral(controllerId);

        json.writeKey("controllerName");
        json.writeStringLiteral(controllerName);
    }

    @Override
    public void init(NSysMonApi sysMon) {

    }
}
