package com.nsysmon.config.log;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import com.nsysmon.config.ADefaultConfigFactory;


/**
 * @author arno
 */
public abstract class NSysMonLogger {
    public abstract void debug(AFunction0NoThrow<String> msg);
    public abstract void info(String msg);
    public abstract void warn(String msg);
    public abstract void warn(String msg, Exception exc);
    public abstract void error(String msg);
    public abstract void error(Exception exc);
    public abstract void error(String msg, Exception exc);

    /**
     * This method is the single point of access for loggers in N-SysMon
     */
    public static NSysMonLogger get(Class<?> context) {
        return ADefaultConfigFactory.getConfiguredLogger().getLogger(context);
    }
}
