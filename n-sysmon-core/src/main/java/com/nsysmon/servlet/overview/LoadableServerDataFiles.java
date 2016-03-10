package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
        FileReader fr = new FileReader(path.toFile());

        copyData(fr, json.getOut());

        json.getOut().flush();
        fr.close();
    }

    private void copyData(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1024];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    private void getFilesAsJson(List<String> params, AJsonSerHelper json) throws IOException {
        json.startObject();
        json.writeKey("files");
        json.startArray();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(NSysMon.get().getConfig().pathDatafiles))) {
            for (Path path : directoryStream) {
                if (!path.getFileName().toString().startsWith(DataFileGeneratorSupporter.DATAFILE_PREFIX)) {
                    continue;
                }
                json.startObject();
                json.writeKey("name");
                json.writeStringLiteral(path.getFileName().toString());
                fillJavaScriptInfos(json, path.getFileName().toString());
                json.endObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        json.endArray();
        json.endObject();

    }

    private void fillJavaScriptInfos(AJsonSerHelper json, String filename) throws IOException {
        String linkString = new DataFileTools().getNsysmonControllerIdFromFilename(filename);
        if (linkString != null) {
            json.writeKey("processor");
            json.writeStringLiteral(linkString);
        }
    }

    @Override
    public void init(NSysMonApi sysMon) {

    }
}
