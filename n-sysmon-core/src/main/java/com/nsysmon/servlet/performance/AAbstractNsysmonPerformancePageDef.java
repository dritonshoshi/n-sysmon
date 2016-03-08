package com.nsysmon.servlet.performance;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.io.IOException;
import java.util.List;


/**
 * @author arno
 */
public abstract class AAbstractNsysmonPerformancePageDef implements APresentationPageDefinition {

    @Override public String getHtmlFileName() {
        return "aggregated.html";
    }

    @Override public String getControllerName() {
        return "CtrlAggregated";
    }

    @Override public void init(NSysMonApi sysMon) {
    }

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws IOException {
        if (SERVICE_GET_DATA.equals(service)) {
            serveAllData(json);
            return true;
        } else if (SERVICE_DO_START.equals(service)) {
            doStartMeasurements();
            serveAllData(json);
            return true;
        } else if (SERVICE_DO_STOP.equals(service)) {
            doStopMeasurements();
            serveAllData(json);
            return true;
        } else if (SERVICE_DO_CLEAR.equals(service)) {
            doClearMeasurements();
            serveAllData(json);
            return true;
        } else if (SERVICE_GET_DATA_DETAIL.equals(service)) {
            serveDataDetail(json, params);
            return true;
        } else if (SERVICE_GET_DATA_OVERVIEW.equals(service)) {
            serveDataOverview(json);
            return true;
        }

        return false;
    }

    protected abstract void doStartMeasurements();
    protected abstract void doStopMeasurements();
    protected abstract void doClearMeasurements();

    protected abstract boolean isStarted();
    protected abstract List<ColDef> getColDefs();
    protected abstract List<TreeNode> getAllData();

    private void serveDataOverview(AJsonSerHelper json) throws IOException {
        addHeaderData(json);

        json.writeKey("traces");
        json.startArray();
        //TODO FOX088S change this from getAllData to something not generating the children, because it would be way faster
        for(TreeNode n: getAllData()) {
            writeOverviewNode(json, n);
        }
        json.endArray();

        json.endObject();
    }

    private void serveDataDetail(AJsonSerHelper json, List<String> params) throws IOException {
        //System.out.println("serveDataDetail:"+params.size());
        System.out.println("serveDataDetail:"+params);
        addHeaderData(json);

        json.writeKey("traces");
        json.startArray();
        //TODO FOX088S change this from getAllData to something not generating the children, because it would be way faster
        for(TreeNode n: getAllData()) {
            params.forEach(s -> {
                if (s.equalsIgnoreCase(n.id)) {
                    try {
                        writeAllDataNode(json, n);
                    } catch (IOException e) {
                        //TODO FOX088S send this to the user
                        e.printStackTrace();
                    }
                }
            });
        }
        json.endArray();

        json.endObject();
    }

    private void addHeaderData(AJsonSerHelper json) throws IOException {
        json.startObject();

        json.writeKey("isStarted");
        json.writeBooleanLiteral(isStarted());

        json.writeKey("columnDefs");
        json.startArray();
        for(ColDef colDef: getColDefs()) {
            writeColDef(json, colDef);
        }
        json.endArray();
    }

    private void serveAllData(AJsonSerHelper json) throws IOException {
        addHeaderData(json);

        json.writeKey("traces");
        json.startArray();
        for(TreeNode n: getAllData()) {
            writeAllDataNode(json, n);
        }
        json.endArray();

        json.endObject();
    }

    private void writeColDef(AJsonSerHelper json, ColDef colDef) throws IOException {
        json.startObject();

        json.writeKey("name");
        json.writeStringLiteral(colDef.name);

        json.writeKey("isPercentage");
        json.writeBooleanLiteral(colDef.isPercentage);

        json.writeKey("numFracDigits");
        json.writeNumberLiteral(colDef.numFracDigits, 0);

        json.writeKey("width");
        json.writeStringLiteral(colDef.width.name());

        json.endObject();
    }

    private void writeOverviewNode(AJsonSerHelper json, TreeNode node) throws IOException {
        json.startObject();

        if(node.id != null) {
            json.writeKey("id");
            json.writeStringLiteral(node.id);
        }

        json.writeKey("name");
        json.writeStringLiteral(node.label);

        if (node.wasKilled) {
            json.writeKey("wasKilled");
            json.writeBooleanLiteral(node.wasKilled);
            //System.out.println(node.wasKilled);
        }

        if(node.tooltip != null && NSysMon.get().getConfig().collectTooltips()) {
            json.writeKey("tooltip");
            json.startArray();

            for(List<String> row: node.tooltip) {

                if (row.size() == 2){
                    json.startObject();
                    json.writeKey("id");
                    json.writeStringLiteral(row.get(0));
                    json.writeKey("value");
                    json.writeStringLiteral(row.get(1));
                    json.endObject();
                }else{
                    json.startArray();
                    for(String cell: row) {
                        json.writeStringLiteral(cell);
                    }
                    json.endArray();
                }

            }

            json.endArray();
        }

        json.writeKey("data");
        json.startArray();
        for(int i=0; i<node.colDataRaw.length; i++) {
            json.writeNumberLiteral(node.colDataRaw[i], getColDefs().get(i).numFracDigits);
        }
        json.endArray();

        if (!node.isSerial) {
            json.writeKey("isNotSerial");
            json.writeBooleanLiteral(true);
        }

        if (!node.children.isEmpty()) {
            json.writeKey("canLoadChildren");
            json.writeBooleanLiteral(true);
        }

        json.endObject();
    }

    private void writeAllDataNode(AJsonSerHelper json, TreeNode node) throws IOException {
        json.startObject();

        if(node.id != null) {
            json.writeKey("id");
            json.writeStringLiteral(node.id);
        }

        json.writeKey("name");
        json.writeStringLiteral(node.label);

        if (node.wasKilled) {
            json.writeKey("wasKilled");
            json.writeBooleanLiteral(node.wasKilled);
            //System.out.println(node.wasKilled);
        }

        if(node.tooltip != null && NSysMon.get().getConfig().collectTooltips()) {
            json.writeKey("tooltip");
            json.startArray();

            for(List<String> row: node.tooltip) {

                if (row.size() == 2){
                    json.startObject();
                    json.writeKey("id");
                    json.writeStringLiteral(row.get(0));
                    json.writeKey("value");
                    json.writeStringLiteral(row.get(1));
                    json.endObject();
                }else{
                    json.startArray();
                    for(String cell: row) {
                        json.writeStringLiteral(cell);
                    }
                    json.endArray();
                }

            }

            json.endArray();
        }

        //tweek to reduce the size of the json file
        if (!node.isSerial) {
            json.writeKey("isNotSerial");
            json.writeBooleanLiteral(true);
        }

        json.writeKey("data");
        json.startArray();
        for(int i=0; i<node.colDataRaw.length; i++) {
            json.writeNumberLiteral(node.colDataRaw[i], getColDefs().get(i).numFracDigits);
        }
        json.endArray();

        if(! node.children.isEmpty()) {
            json.writeKey("children");
            json.startArray();
            for(TreeNode child: node.children) {
                writeAllDataNode(json, child);
            }
            json.endArray();
        }

        json.endObject();
    }

    protected enum ColWidth {Short, Medium, Long}
    protected static class ColDef {
        public final String name;
        public final boolean isPercentage;
        public final int numFracDigits;
        public final ColWidth width;

        public ColDef(String name, boolean isPercentage, int numFracDigits, ColWidth width) {
            this.name = name;
            this.isPercentage = isPercentage;
            this.numFracDigits = numFracDigits;
            this.width = width;
        }
    }

    protected static class TreeNode {
        public final String id;
        public final String label;
        public final List<List<String>> tooltip; // list of rows, each row is a list of columns
        public final boolean isSerial;
        public final long[] colDataRaw;
        public final List<TreeNode> children;
        public boolean wasKilled;

        public TreeNode(String label, boolean isSerial, long[] colDataRaw, List<TreeNode> children) {
            this(null, label, null, isSerial, colDataRaw, children, false);
        }

        public TreeNode(String id, String label, List<List<String>> tooltip, boolean isSerial, long[] colDataRaw, List<TreeNode> children, boolean wasKilled) {
            this.id = id == null ? null : id.intern();
            this.label = label == null ? null : label.intern();
            this.tooltip = tooltip;
            this.isSerial = isSerial;
            this.colDataRaw = colDataRaw;
            this.children = children;
            this.wasKilled = wasKilled;
        }
    }
}
