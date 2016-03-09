package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class LoadableServerDataFiles implements APresentationPageDefinition {

    @Override public String getId() {
        return "loadableServerDataFiles";
    }

    @Override public String getShortLabel() {
        return "Datafiles";
    }

    @Override public String getFullLabel() {
        return "List Data Files on Server";
    }

    @Override public String getHtmlFileName() {
        return "loadableserverdatafiles.html";
    }

    @Override public String getControllerName() {
        return "CtrlLoadableServerDataFiles";
    }

    private final Path inputPath = Paths.get("/tmp/");//TODO FOX088S move this to config

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception {
        if ("getFiles".equals(service)) {
            getFilesAsJson(params, json);
            return true;
        }
        return false;
    }

    private void getFilesAsJson(List<String> params, AJsonSerHelper json) throws IOException {
        json.startObject();
        json.writeKey("files");
        json.startArray();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputPath)) {
            for (Path path : directoryStream) {
                if (!path.getFileName().toString().startsWith("tkt_")){
                    continue;
                }
                json.startObject();
                json.writeKey("name");
                json.writeStringLiteral(path.getFileName().toString());
                json.endObject();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        json.endArray();
        json.endObject();

    }

    @Override public void init(NSysMonApi sysMon) {

    }
}
