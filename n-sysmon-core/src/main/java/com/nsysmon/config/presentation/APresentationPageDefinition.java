package com.nsysmon.config.presentation;

import com.ajjpj.afoundation.io.AJsonSerHelper;
import com.nsysmon.NSysMonApi;

import java.util.List;


/**
 * This interface represents the entire definition of one "view" in NSysMon's HTML interface. Everything else is
 *  done dynamically based on instances of this interface.
 *
 * @author arno
 */
public interface APresentationPageDefinition {
    String SERVICE_GET_DATA = "getData";
    String SERVICE_GET_DATA_OVERVIEW = "getDataOverview";
    String SERVICE_GET_DATA_DETAIL = "getDataDetail";
    String SERVICE_DO_START = "doStart";
    String SERVICE_DO_STOP = "doStop";
    String SERVICE_DO_CLEAR = "doClear";

    /**
     * @return a logical ID that is used as a representation for this instance. It must be unique across all instances.
     *  It may contain only (ASCII) characters, digits, the underscore and the dash. It is case sensitive.
     */
    String getId();

    /**
     * @return a short human readable title of this instance, used e.g. for its menu entry.
     */
    String getShortLabel();

    /**
     * @return a (potentially) more verbose title for this instance, used e.g. in a heading on the page.
     */
    String getFullLabel();

    /**
     * @return the name of the (partial) HTML file used to render this page
     */
    String getHtmlFileName();

    /**
     * @return the name of the AngluarJS controller for this page
     */
    String getControllerName();

    /**
     * This method must render the JSON response for a RESTful service call. Callers route calls to the appropriate
     *  page definition by its ID, so the method is called only for service calls intended for this instance.
     *
     * @return true iff the service could be handled
     */
    boolean handleRestCall(String service, List<String> params, AJsonSerHelper json) throws Exception;

    /**
     * called once before any of the other methods is called.
     */
    void init(NSysMonApi sysMon);
}
