package com.nsysmon.measure.http;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author arno
 */
public interface AHttpRequestAnalyzer {
    AHttpRequestDetails analyze(HttpServletRequest request);
}
