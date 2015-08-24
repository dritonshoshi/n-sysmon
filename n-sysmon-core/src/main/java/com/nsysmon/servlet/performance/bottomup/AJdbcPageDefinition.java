package com.nsysmon.servlet.performance.bottomup;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.measure.jdbc.NSysMonStatement;


/**
 * @author arno
 */
public class AJdbcPageDefinition extends ABottomUpPageDefinition {
    @Override protected ABottomUpLeafFilter createLeafFilter() {
        return new ABottomUpLeafFilter() {
            @Override public boolean isLeaf(AHierarchicalData data) {
                return data.getIdentifier().startsWith(NSysMonStatement.IDENT_PREFIX_JDBC) &&
                        !data.getIdentifier ().startsWith (NSysMonStatement.IDENT_PREFIX_JDBC + "connection");
            }
        };
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
