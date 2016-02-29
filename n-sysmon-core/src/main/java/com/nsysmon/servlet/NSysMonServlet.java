package com.nsysmon.servlet;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.presentation.APresentationMenuEntry;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @author arno
 */
public class NSysMonServlet extends AbstractNSysMonServlet {
    public static final String CONFIG_JS = "config.js";

    private final Map<String, APresentationPageDefinition> pageDefs = new ConcurrentHashMap<String, APresentationPageDefinition>();

    @Override
    public void init() throws ServletException {
        super.init();
        for(APresentationMenuEntry menuEntry: getSysMon().getConfig().presentationMenuEntries) {
            for(APresentationPageDefinition pageDef: menuEntry.pageDefinitions) {
                final Object prev = pageDefs.put(pageDef.getId(), pageDef);
                if(prev != null) {
                    throw new IllegalStateException("more than one page definitions with id '" + pageDef.getId() + "'");
                }
            }
        }
    }

    @Override protected String getDefaultHtmlName() {
        return "nsysmon.html";
    }

    @Override protected boolean handleRestCall(List<String> restParams, HttpServletResponse resp) throws Exception {
        //TODO This results in the JSON being one huge line. Find a way to add newlines so other tools can open the file
        final AJsonSerHelper json = new AJsonSerHelper(resp.getOutputStream());

        final String pageId = restParams.remove(0);
        final String service = restParams.remove(0);

        final APresentationPageDefinition pageDef = pageDefs.get(pageId);
        if(pageDef == null) {
            throw new IllegalArgumentException("no page def with ID '" + pageId + "'");
        }
        return pageDef.handleRestCall(service, restParams, json);
    }

    @Override protected boolean handleDynamic(List<String> pathSegments, HttpServletResponse resp) throws IOException {
        if(CONFIG_JS.equals(pathSegments.get(0))) {
            serveConfig(resp);
            return true;
        }

        return false;
    }

    /**
     * override to customize
     */
    protected NSysMonApi getSysMon() {
        return NSysMon.get();
    }

    private void serveConfig(HttpServletResponse resp) throws IOException {
        final NSysMonConfig config = getSysMon().getConfig();

        final ServletOutputStream out = resp.getOutputStream();

        out.print("angular.module('NSysMonApp').constant('configRaw', ");
        out.flush();

        final AJsonSerHelper json = new AJsonSerHelper(out);
        json.startObject();

        json.writeKey("applicationId");
        json.writeStringLiteral(config.appInfo.getApplicationName());

        json.writeKey("applicationDeployment");
        json.writeStringLiteral(config.appInfo.getDeployment());

        json.writeKey("applicationNode");
        json.writeStringLiteral(config.appInfo.getNodeId());

        json.writeKey("applicationVersion");
        json.writeStringLiteral(config.appInfo.getVersion());

        json.writeKey("applicationInstanceHtmlColorCode");
        json.writeStringLiteral(config.appInfo.getHtmlColorCode());

        json.writeKey("defaultPage");
        json.writeStringLiteral(config.defaultPage);

        json.writeKey("menuEntries");
        json.startArray();
        for(APresentationMenuEntry menuEntry: config.presentationMenuEntries) {
            writeMenuEntry(menuEntry, json);
        }
        json.endArray();

        json.endObject();

        out.println(");");
    }

    private void writeMenuEntry(APresentationMenuEntry menuEntry, AJsonSerHelper json) throws IOException {
        json.startObject();

        json.writeKey("label");
        json.writeStringLiteral(menuEntry.label);

        json.writeKey("entries");
        json.startArray();

        for(APresentationPageDefinition pageDef: menuEntry.pageDefinitions) {
            json.startObject();

            json.writeKey("id");
            json.writeStringLiteral(pageDef.getId());

            json.writeKey("controller");
            json.writeStringLiteral(pageDef.getControllerName());

            json.writeKey("htmlFileName");
            json.writeStringLiteral(pageDef.getHtmlFileName());

            json.writeKey("shortLabel");
            json.writeStringLiteral(pageDef.getShortLabel());

            json.writeKey("fullLabel");
            json.writeStringLiteral(pageDef.getFullLabel());

            json.endObject();
        }

        json.endArray();

        json.endObject();
    }
}
