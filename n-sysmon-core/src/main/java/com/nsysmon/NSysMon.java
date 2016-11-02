package com.nsysmon;

import com.nsysmon.config.ADefaultConfigFactory;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.impl.NSysMonImpl;
import com.ajjpj.afoundation.function.AFunction0;
import com.ajjpj.afoundation.util.AUnchecker;


/**
 * This class is the point of contact for an application to NSysMon. There are basically two ways to use it:
 *
 * <ul>
 *     <li> Use the static get() method to access it as a singleton. That is simple and convenient, and it is
 *          sufficient for many applications. If it is used that way, all configuration must be done through
 *          the static methods of ADefaultSysMonConfig. </li>
 *     <li> Create and manage your own instance (or instances) by calling the NSysMonImpl constructor, passing in your
 *          configuration. This is for maximum flexibility, but you lose some convenience. </li>
 * </ul>
 *
 * @author arno
 */
public class NSysMon {
    public static NSysMonApi get() {
        // this class has the sole purpose of providing really lazy init of the singleton instance
        return NSysMonInstanceHolder.INSTANCE;
    }

    public static boolean isWindows(){
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
    /**
     * this class has the sole purpose of providing really lazy init of the singleton instance
     */
    private static class NSysMonInstanceHolder {
        public static final NSysMonApi INSTANCE = new NSysMonImpl(getConfig());

        private static NSysMonConfig getConfig() {
            return AUnchecker.executeUnchecked((AFunction0<NSysMonConfig, Exception>) () -> ADefaultConfigFactory.getConfigFactory().getConfig());
        }
    }
}

