package com.nsysmon.config.wiring;

import com.nsysmon.config.log.NSysMonLoggerFactory;
import com.nsysmon.testutil.CountingLoggerFactory;
import org.junit.jupiter.api.BeforeEach;

import java.util.Properties;

/**
 * Shared setup for the {@link ConfigPropsFile} / {@link ConfigValueResolver} tests.
 */
abstract class ConfigPropsFileTestBase {

    protected Properties props;

    @BeforeEach
    void before() {
        props = new Properties();
    }

    protected ConfigPropsFile configFile() {
        return configFile(props);
    }

    protected ConfigPropsFile configFile(Properties source) {
        return new ConfigPropsFile(source, loggerFactory());
    }

    protected NSysMonLoggerFactory loggerFactory() {
        return new CountingLoggerFactory();
    }
}