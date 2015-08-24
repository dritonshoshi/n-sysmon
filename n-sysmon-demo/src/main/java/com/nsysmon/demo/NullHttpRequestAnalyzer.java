package com.nsysmon.demo;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.measure.http.ASimpleHttpRequestAnalyzer;

/**
 * This class removes the default behavior of aggregating HTTP request. It is
 *
 *   JUST FOR THIS DEMO - DO NOT DO THIS FOR PRODUCTION CODE!!!
 *
 *
 * @author arno
 */
public class NullHttpRequestAnalyzer extends ASimpleHttpRequestAnalyzer {
    @Override protected AOption<String> extractIdentifier(String url) {
        if(url.contains("nsysmon")) {
            return AOption.none();
        }

        return AOption.some(url);
    }
}
