package com.nsysmon.servlet.performance;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
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

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) throws IOException {
        if("getData".equals(service)) {
            serveData(json);
            return true;
        }
        if("doStart".equals(service)) {
            doStartMeasurements();
            serveData(json);
            return true;
        }
        if("doStop".equals(service)) {
            doStopMeasurements();
            serveData(json);
            return true;
        }
        if("doClear".equals(service)) {
            doClearMeasurements();
            serveData(json);
            return true;
        }

        return false;
    }

    protected abstract void doStartMeasurements();
    protected abstract void doStopMeasurements();
    protected abstract void doClearMeasurements();

    protected abstract boolean isStarted();
    protected abstract List<ColDef> getColDefs();
    protected abstract List<TreeNode> getData();

    protected void serveData(AJsonSerHelperForNSysmon json) throws IOException {
        json.startObject();

        json.writeKey("isStarted");
        json.writeBooleanLiteral(isStarted());

        json.writeKey("columnDefs");
        json.startArray();
        for(ColDef colDef: getColDefs()) {
            writeColDef(json, colDef);
        }
        json.endArray();

        json.writeKey("traces");
        json.startArray();
        int nr = 0;
        for(TreeNode n: getData()) {
            writeDataNode(json, n, nr++);
        }
        json.endArray();

        json.endObject();
    }

    private void writeColDef(AJsonSerHelperForNSysmon json, ColDef colDef) throws IOException {
        json.startObject();

        json.writeKey("name");
        json.writeStringLiteral(colDef.name);

        json.writeKey("isPercentage");
        json.writeBooleanLiteral(colDef.isPercentage);

        json.writeKey("isTimestamp");
        json.writeBooleanLiteral(colDef.isTimestamp);

        json.writeKey("numFracDigits");
        json.writeNumberLiteral(colDef.numFracDigits, 0);

        json.writeKey("width");
        json.writeStringLiteral(colDef.width.name());

        json.endObject();
    }

    private void writeDataNode(AJsonSerHelperForNSysmon json, TreeNode node, int nr) throws IOException {
        json.startObject();

        if(node.id != null) {
            json.writeKey("id");
            //this is done to reduce the size of the json, because now we don'T send the 20 chars per id, but a smaller string
//            json.writeStringLiteral("n"+nr);
            json.writeStringLiteral(node.id);
        }

        json.writeKey("name");
        json.writeStringLiteral(node.label);

        if (node.wasKilled) {
            json.writeKey("wasKilled");
            json.writeBooleanLiteral(node.wasKilled);
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
                writeDataNode(json, child, nr++);
            }
            json.endArray();
        }

        json.endObject();
    }

    protected enum ColWidth {Short, Medium, Long}
    protected static class ColDef {
        public final String name;
        public final boolean isPercentage;
        public final boolean isTimestamp;
        public final int numFracDigits;
        public final ColWidth width;

        public ColDef(String name, boolean isPercentage, int numFracDigits, ColWidth width, boolean isTimestamp) {
            this.name = name;
            this.isPercentage = isPercentage;
            this.numFracDigits = numFracDigits;
            this.width = width;
            this.isTimestamp = isTimestamp;
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

        public TreeNode(final String id,final  String label,final  List<List<String>> tooltip,final  boolean isSerial, long[] colDataRaw, List<TreeNode> children, boolean wasKilled) {
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
