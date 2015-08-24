package com.nsysmon.server.init;

import com.nsysmon.server.services.AdminService;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author arno
 */
public class NSysMonJaxRsApplication extends ResourceConfig {
    public NSysMonJaxRsApplication() {
        register(JacksonFeature.class);

        registerInstances(
                InitServletContextListener.getInjector().getInstance(AdminService.class)
        );
    }
}
