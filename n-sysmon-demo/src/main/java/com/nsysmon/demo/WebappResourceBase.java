package com.nsysmon.demo;

import java.io.File;

/**
 * Resolves the directory that the Jetty demo serves as its webapp.
 * <p>
 * The webapp content - the deployment descriptor, the demo servlet, the n-sysmon configuration -
 * lives in the {@code n-sysmon-demo-webapp} module, which both demo runners share: this Jetty one
 * serves it straight out of the source tree so that there is nothing to package first, the Liberty
 * one packages the very same content into a war.
 *
 * @author arno
 */
final class WebappResourceBase {
    static final String WEBAPP_RESOURCE_PATH = "n-sysmon-demo-webapp/src/main/resources";

    private WebappResourceBase() {
        // static helper
    }

    /**
     * @param workingDirectory the directory the demo was started from, normally the repository root
     * @return the webapp resource directory, relative to {@code workingDirectory}
     * @throws IllegalStateException if that directory does not exist, naming what to do about it
     */
    static File resolve(File workingDirectory) {
        final File result = new File(workingDirectory, WEBAPP_RESOURCE_PATH);
        if(! result.isDirectory()) {
            throw new IllegalStateException("webapp resource directory not found: " + result.getAbsolutePath()
                    + ". Start the demo from the repository root of a full checkout: the demo serves the webapp "
                    + "of the sibling module '" + WEBAPP_RESOURCE_PATH + "'.");
        }
        return result;
    }
}
