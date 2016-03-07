package com.nsysmon.config;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.function.AFunction0;
import com.ajjpj.afoundation.util.AUnchecker;
import com.nsysmon.config.appinfo.AApplicationInfoProvider;
import com.nsysmon.config.log.NLog4JLoggerFactory;
import com.nsysmon.config.log.NStdOutLoggerFactory;
import com.nsysmon.config.log.NSysMonLoggerFactory;
import com.nsysmon.config.presentation.APresentationPageDefinition;
import com.nsysmon.config.wiring.ConfigPropsFile;
import com.nsysmon.datasink.ADataSink;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.http.AHttpRequestAnalyzer;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.util.timer.ATimer;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import com.ajjpj.afoundation.util.AUnchecker;


/**
 * This class evaluates the default configuration files, creating a config instance from their content.
 *
 * @author arno
 */
public class ADefaultConfigFactory implements AConfigFactory {
    public static final String DEFAULT_CONFIG_FILE = "nsysmon-default.properties";
    public static final String CONFIG_FILE = "nsysmon.properties";
    public static final String SYSPROP_PREFIX = "nsysmon.";

    public static final String KEY_CONFIG_FACTORY = "config-factory";
    public static final String KEY_LOGGER = "logger";
    public static final String KEY_TIMER = "timer";
    public static final String KEY_HTTP_REQUEST_ANALYZER = "http-request-analyzer";

    public static final String KEY_DEFAULT_PAGE = "default-page";

    public static final String KEY_ENV_MEASURERS = "env-measurers";
    public static final String KEY_SCALAR_MEASURERS = "scalar-measurers";
    public static final String KEY_TIMED_SCALAR_MEASURERS = "timedscalar-measurers";
    public static final String KEY_DATA_SINKS = "data-sinks";
    public static final String KEY_PRESENTATION_MENUS = "presentation-menus";

    public static final String KEY_AVERAGING_DELAY_FOR_SCALARS_MILLIS = "averaging-delay-for-scalars-millis";

    public static final String KEY_MEASUREMENT_TIMEOUT_NANOS = "measurement-timeout-nanos";
    public static final String KEY_MAX_NUM_MEASUREMENT_TIMEOUTS = "max-num-measurement-timeouts";

    public static final String KEY_MAX_NESTED_MEASUREMENTS = "max-nested-measurements";
    public static final String KEY_MAX_NUM_MEASUREMENTS_PER_HIERARCHY = "max-measurements-per-hierarchy";

    public static final String KEY_MAX_NUM_MEASUREMENTS_PER_TIMED_SCALAR = "max-measurements-per-timed-scalar";
    public static final String KEY_DURATION_OF_ONE_TIMED_SCALAR = "duration-of-one-timed-scalar";

    public static final String KEY_COLLECT_SQL_PARAMETERS = "collect-sql-parameters";
    public static final String KEY_COLLECT_TOOLTIPS = "collect-tooltips";

    public static final String KEY_DATA_SINK_TIMEOUT_NANOS = "data-sink-timeout-nanos";
    public static final String KEY_MAX_NUM_DATA_SINK_TIMEOUTS = "max-num-data-sink-timeouts";

    public static final String KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR = "tomcat-global-request-processor";

    private static volatile NSysMonLoggerFactory configuredLogger;

    public static AConfigFactory getConfigFactory() {
        return AUnchecker.executeUnchecked((AFunction0<AConfigFactory, Exception>) () -> {
            final Properties propsRaw = getProperties();

            if (configuredLogger == null) {
                // allow API override for config file settings
                configuredLogger = extractLogger(propsRaw);
            }
            final ConfigPropsFile props = new ConfigPropsFile(propsRaw, extractLogger(propsRaw));
            return props.get(KEY_CONFIG_FACTORY, AOption.some(new ADefaultConfigFactory()), AConfigFactory.class);
        });
    }

    /**
     * This method allows 'manual' override for settings in the config files. The only known use of this is testability.
     *  This method must be called before N-SysMon itself is initialized in order to be effective.
     */
    public static void setConfiguredLogger(NSysMonLoggerFactory logger) {
        configuredLogger = logger;
    }

    public static NSysMonLoggerFactory getConfiguredLogger() {
        if(configuredLogger == null) {
            configuredLogger = extractLogger(getProperties());
        }
        return configuredLogger;
    }

    private static NSysMonLoggerFactory extractLogger(Properties props) {
        final String loggerClassName = props.getProperty(KEY_LOGGER);
        try {
            if(loggerClassName == null) {
                return defaultLogger(); // avoid the warning log entry
            }

            return (NSysMonLoggerFactory) Class.forName(loggerClassName.trim()).newInstance();
        }
        catch (Exception exc) {
            final NSysMonLoggerFactory logger = defaultLogger();
            logger.getLogger(ADefaultConfigFactory.class).warn("exception creating logger based on config file entry '" + loggerClassName + "': " + exc);
            return logger;
        }
    }

    private static NSysMonLoggerFactory defaultLogger() {
        try {
            return NLog4JLoggerFactory.INSTANCE;
        }
        catch (Throwable th) {
            return NStdOutLoggerFactory.INSTANCE;
        }
    }


    private static Properties getProperties() {
        try {
            final Properties result = new Properties();

            result.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE));

            final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE);
            if(in != null) {
                result.load(in);
            }

            for(String sysProp: System.getProperties().stringPropertyNames()) {
                if(! sysProp.startsWith(SYSPROP_PREFIX)) {
                    continue;
                }
                final String reducedKey = sysProp.substring(SYSPROP_PREFIX.length());
                result.setProperty(reducedKey, System.getProperty(sysProp));
            }

            return result;
        } catch (Exception e) {
            AUnchecker.throwUnchecked(e);
            return null; // for the compiler
        }
    }

    public NSysMonConfig getConfig() {
        final ConfigPropsFile props = new ConfigPropsFile(getProperties(), getConfiguredLogger());

        final AApplicationInfoProvider appInfo = props.get("application-info", AApplicationInfoProvider.class);
        final NSysMonConfigBuilder builder = new NSysMonConfigBuilder(appInfo);
        builder.setTimer(props.get(KEY_TIMER, ATimer.class));
        builder.setHttpRequestAnalyzer(props.get(KEY_HTTP_REQUEST_ANALYZER, AHttpRequestAnalyzer.class));

        builder.setDefaultPage(props.get(KEY_DEFAULT_PAGE, String.class));

        builder.setAveragingDelayForScalarsMillis(props.get(KEY_AVERAGING_DELAY_FOR_SCALARS_MILLIS, Integer.TYPE));

        builder.setMeasurementTimeoutNanos(props.get(KEY_MEASUREMENT_TIMEOUT_NANOS, Long.TYPE));
        builder.setMaxNumMeasurementTimeouts(props.get(KEY_MAX_NUM_MEASUREMENT_TIMEOUTS, Integer.TYPE));

        builder.setDurationOfOneTimedScalar(props.get(KEY_DURATION_OF_ONE_TIMED_SCALAR, Integer.TYPE));

        builder.setCollectSqlParameters(props.get(KEY_COLLECT_SQL_PARAMETERS, Boolean.TYPE));
        builder.setCollectTooltips(props.get(KEY_COLLECT_TOOLTIPS, Boolean.TYPE));

        builder.setMaxNestedMeasurements(props.get(KEY_MAX_NESTED_MEASUREMENTS, Integer.TYPE));
        builder.setMaxNumMeasurementsPerHierarchy(props.get(KEY_MAX_NUM_MEASUREMENTS_PER_HIERARCHY, Integer.TYPE));
        builder.setMaxNumMeasurementsPerTimedScalar(props.get(KEY_MAX_NUM_MEASUREMENTS_PER_TIMED_SCALAR, Integer.TYPE));

        builder.setDataSinkTimeoutNanos (props.get (KEY_DATA_SINK_TIMEOUT_NANOS, Long.TYPE));
        builder.setMaxNumDataSinkTimeouts(props.get(KEY_MAX_NUM_DATA_SINK_TIMEOUTS, Integer.TYPE));

        //TODO this need to be refactored, so measurements can be configured
        Map<String, String> additionalConfigurations = new HashMap<>();
        additionalConfigurations.put(KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR, props.get(KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR, String.class));

        builder.setAdditionalConfigurationParameters(additionalConfigurations);

        props.getList(KEY_ENV_MEASURERS, AEnvironmentMeasurer.class).forEach(builder::addEnvironmentMeasurer);

        props.getList(KEY_SCALAR_MEASURERS, AScalarMeasurer.class).forEach(builder::addScalarMeasurer);

        props.getList(KEY_TIMED_SCALAR_MEASURERS, AScalarMeasurer.class).forEach(builder::addScalarTimedMeasurer);

        props.getList(KEY_DATA_SINKS, ADataSink.class).forEach(builder::addDataSink);

        for(String menuEntryRaw: props.getListRaw(KEY_PRESENTATION_MENUS)) {
            final String menuEntry = menuEntryRaw.trim();

            final List<APresentationPageDefinition> pageDefs = props.getList(KEY_PRESENTATION_MENUS + '.' + menuEntry, APresentationPageDefinition.class);
            builder.addPresentationMenuEntry(menuEntry, pageDefs);
        }

        return builder.build();
    }
}
