package com.nsysmon.servlet.overview;

import java.io.IOException;
import java.io.OutputStream;

public interface DataFileGeneratorSupporter {
    void getDataForExport(OutputStream os) throws IOException;
}
