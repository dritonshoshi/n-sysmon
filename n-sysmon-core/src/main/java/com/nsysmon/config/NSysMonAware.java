package com.nsysmon.config;

import com.nsysmon.NSysMonApi;

/**
 * If a measurers or data sink implements this interface and is registered with an N-SysMon instance, it will be
 *  injected that N-SysMon instance as part of the registration process.
 *
 * @author arno
 */
public interface NSysMonAware {
    void setNSysMon(NSysMonApi sysMon);
}
