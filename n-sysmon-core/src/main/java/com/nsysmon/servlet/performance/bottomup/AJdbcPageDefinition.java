package com.nsysmon.servlet.performance.bottomup;

import com.nsysmon.measure.jdbc.NSysMonStatement;


/**
 * @author arno
 */
public class AJdbcPageDefinition extends ABottomUpPageDefinition {
    @Override protected ABottomUpLeafFilter createLeafFilter() {
        return data -> data.getIdentifier().startsWith(NSysMonStatement.IDENT_PREFIX_JDBC) &&
                !data.getIdentifier ().startsWith (NSysMonStatement.IDENT_PREFIX_JDBC + "connection");
    }


    @Override public String getId() {
        return "jdbc";
    }

    @Override public String getShortLabel() {
        return "JDBC";
    }

    @Override public String getFullLabel() {
        return "JDBC Performance Statistics";
    }
}
