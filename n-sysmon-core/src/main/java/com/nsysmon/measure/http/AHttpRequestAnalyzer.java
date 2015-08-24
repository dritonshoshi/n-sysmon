package com.nsysmon.measure.http;

import javax.servlet.http.HttpServletRequest;

/**
 * @author arno
 */
public interface AHttpRequestAnalyzer {
    AHttpRequestDetails analyze(HttpServletRequest request);
}
