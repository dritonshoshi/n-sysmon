package com.nsysmon.testutil;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.presentation.APresentationPageDefinition;

import java.util.List;

/**
 * A page definition that only knows its labels, for tests that need a page registered in the presentation menu but
 *  never call into it.
 */
public class StubPageDefinition implements APresentationPageDefinition {
    private final String id;

    public StubPageDefinition(String id) {
        this.id = id;
    }

    @Override public String getId() {
        return id;
    }

    @Override public String getShortLabel() {
        return id + " Short";
    }

    @Override public String getFullLabel() {
        return id + " Full";
    }

    @Override public String getHtmlFileName() {
        return id + ".html";
    }

    @Override public String getControllerName() {
        return "Ctrl" + id;
    }

    @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) {
        return false;
    }

    @Override public void init(NSysMonApi sysMon) {
    }
}
