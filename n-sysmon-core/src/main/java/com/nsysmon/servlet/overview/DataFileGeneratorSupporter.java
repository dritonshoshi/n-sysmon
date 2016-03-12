package com.nsysmon.servlet.overview;

import java.io.IOException;
import java.io.OutputStream;

public interface DataFileGeneratorSupporter {
    String DATAFILE_PREFIX = "nsysmon";

    void getDataForExport(OutputStream os) throws IOException;

}
