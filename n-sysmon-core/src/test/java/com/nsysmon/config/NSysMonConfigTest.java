package com.nsysmon.config;

import com.nsysmon.config.appinfo.AApplicationInfoProvider;
import com.nsysmon.servlet.auth.AAllowAllWebAuthorizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSysMonConfigTest {

    private static final AApplicationInfoProvider APP_INFO = new AApplicationInfoProvider() {
        @Override public String getApplicationName() {
            return "app";
        }
        @Override public String getVersion() {
            return "1";
        }
        @Override public String getDeployment() {
            return "dev";
        }
        @Override public String getNodeId() {
            return "n";
        }
        @Override public String getHtmlColorCode() {
            return "#abcdef";
        }
    };

    private NSysMonConfig config;

    @BeforeEach
    void before() {
        config = configWithFlags(true, true);
    }

    @AfterEach
    void after() {
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
    }

    private static NSysMonConfig configWithFlags(boolean sqlParameters, boolean tooltips) {
        return new NSysMonConfig(APP_INFO, 1, 2, 3, 4, 5, 6, 7L, 8, 9L, 10,
                null, null, AAllowAllWebAuthorizer.INSTANCE,
                List.of(), List.of(), List.of(), List.of(),
                "page", List.of(), Map.of(), sqlParameters, tooltips,
                "/data", Map.of("k", (Object) "v"), false);
    }

    @Test
    void globallyDisabledIsFalseWhenTheSystemPropertyIsAbsent() {
        assertFalse(NSysMonConfig.isGloballyDisabled());
    }

    @Test
    void globallyDisabledIsTrueForTheExactValueTrue() {
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        assertTrue(NSysMonConfig.isGloballyDisabled());
    }

    @Test
    void globallyDisabledIsFalseForAnyOtherValue() {
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "TRUE");

        assertFalse(NSysMonConfig.isGloballyDisabled());
    }

    @Test
    void configuredFlagsReflectTheConstructorArguments() {
        assertTrue(config.configuredCollectSqlParameters());
        assertTrue(config.configuredCollectTooltips());
    }

    @Test
    void effectiveFlagsFallBackToTheConfiguredOnes() {
        assertTrue(config.collectSqlParameters());
        assertTrue(config.collectTooltips());
    }

    @Test
    void startOverrideSqlParametersForcesTheFlagOn() {
        final NSysMonConfig cfg = configWithFlags(false, false);
        assertFalse(cfg.collectSqlParameters());

        cfg.startOverrideSqlParameters();

        assertTrue(cfg.collectSqlParameters());
    }

    @Test
    void stopOverrideSqlParametersRestoresTheConfiguredFlag() {
        final NSysMonConfig cfg = configWithFlags(false, true);
        cfg.startOverrideSqlParameters();

        cfg.stopOverrideSqlParameters();

        assertFalse(cfg.collectSqlParameters());
    }

    @Test
    void stopOverrideSqlParametersForcesTheFlagOffEvenIfItWasConfiguredOn() throws Exception {
        // NB: 'stopOverrideSqlParameters' assigns false instead of clearing the override, so an
        //     override cycle cannot restore a configured true
        final NSysMonConfig cfg = configWithFlags(true, true);
        assertTrue(cfg.collectSqlParameters());

        cfg.startOverrideSqlParameters();
        cfg.stopOverrideSqlParameters();

        assertFalse(cfg.collectSqlParameters());
    }

    @Test
    void stopOverrideCollectTooltipsForcesTheFlagOffEvenIfItWasConfiguredOn() {
        final NSysMonConfig cfg = configWithFlags(true, true);
        assertTrue(cfg.collectTooltips());

        cfg.startOverrideCollectTooltips();
        cfg.stopOverrideCollectTooltips();

        assertFalse(cfg.collectTooltips());
    }

    @Test
    void startOverrideCollectTooltipsForcesTheFlagOn() {
        final NSysMonConfig cfg = configWithFlags(true, false);
        assertFalse(cfg.collectTooltips());

        cfg.startOverrideCollectTooltips();

        assertTrue(cfg.collectTooltips());
    }

    @Test
    void stopOverrideCollectTooltipsRestoresTheConfiguredFlag() {
        final NSysMonConfig cfg = configWithFlags(true, false);
        cfg.startOverrideCollectTooltips();

        cfg.stopOverrideCollectTooltips();

        assertFalse(cfg.collectTooltips());
    }

    @Test
    void sqlParameterOverrideDoesNotAffectTooltips() {
        final NSysMonConfig cfg = configWithFlags(false, true);

        cfg.startOverrideSqlParameters();

        assertTrue(cfg.collectSqlParameters());
        assertTrue(cfg.collectTooltips());
    }

    @Test
    void collectionsAreExposedUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () -> config.initialScalarMeasurers.add(null));
    }

    @Test
    void scalarParametersMapIsExposed() {
        assertEquals(Map.of("k", (Object) "v"), config.getTimedScalarMonitoringParameters());
    }

    @Test
    void scalarValuesAreKept() {
        assertSame(APP_INFO, config.appInfo);
        assertEquals("/data", config.pathDatafiles);
        assertEquals("page", config.defaultPage);
    }
}
