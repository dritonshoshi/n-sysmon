package com.nsysmon.config.appinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ADefaultApplicationInfoProviderTest {

    @Test
    void gettersReturnTheConfiguredValues() {
        final AApplicationInfoProvider info = new ADefaultApplicationInfoProvider("app", "1.2.3", "prod", "node-7");

        assertEquals("app", info.getApplicationName());
        assertEquals("1.2.3", info.getVersion());
        assertEquals("prod", info.getDeployment());
        assertEquals("node-7", info.getNodeId());
    }

    @Test
    void twoArgConstructorDerivesDeploymentFromUserAndHost() throws Exception {
        final AApplicationInfoProvider info = new ADefaultApplicationInfoProvider("app", "1.2.3");

        assertTrue(info.getDeployment().startsWith(System.getProperty("user.name") + "@"), info.getDeployment());
        assertEquals("the-node", info.getNodeId());
    }

    @Test
    void twoArgConstructorKeepsNameAndVersion() throws Exception {
        final AApplicationInfoProvider info = new ADefaultApplicationInfoProvider("app", "1.2.3");

        assertEquals("app", info.getApplicationName());
        assertEquals("1.2.3", info.getVersion());
    }

    @Test
    void calcDeploymentCombinesUserNameAndHost() throws Exception {
        assertEquals(System.getProperty("user.name") + "@" + java.net.InetAddress.getLocalHost(),
                ADefaultApplicationInfoProvider.calcDeployment());
    }

    @Test
    void htmlColorCodeIsSixHexDigits() {
        final AApplicationInfoProvider info = new ADefaultApplicationInfoProvider("app", "1.2.3", "prod", "node-7");

        assertTrue(info.getHtmlColorCode().matches("#[0-9a-f]{6}"), info.getHtmlColorCode());
    }

    @Test
    void htmlColorCodeIsDerivedFromTheInfoValues() {
        final AApplicationInfoProvider info = new ADefaultApplicationInfoProvider("app", "1.2.3", "prod", "node-7");

        assertEquals(ADefaultApplicationInfoProvider.calcHtmlColor(info), info.getHtmlColorCode());
    }

    @Test
    void htmlColorCodeIsStableForEqualInfo() {
        assertEquals(
                ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "1", "d", "n")),
                ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "1", "d", "n")));
    }

    @Test
    void htmlColorCodeDependsOnEveryValue() {
        final String base = ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "1", "d", "n"));

        assertNotEquals(base, ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app2", "1", "d", "n")));
        assertNotEquals(base, ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "2", "d", "n")));
        assertNotEquals(base, ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "1", "d2", "n")));
        assertNotEquals(base, ADefaultApplicationInfoProvider.calcHtmlColor(new ADefaultApplicationInfoProvider("app", "1", "d", "n2")));
    }

    @Test
    void htmlColorCodeIsComputedForAnyInterfaceImplementation() {
        final AApplicationInfoProvider anonymous = new AApplicationInfoProvider() {
            @Override public String getApplicationName() {
                return "x";
            }
            @Override public String getVersion() {
                return "y";
            }
            @Override public String getDeployment() {
                return "z";
            }
            @Override public String getNodeId() {
                return "w";
            }
            @Override public String getHtmlColorCode() {
                return ADefaultApplicationInfoProvider.calcHtmlColor(this);
            }
        };

        assertEquals(ADefaultApplicationInfoProvider.calcHtmlColor(anonymous), anonymous.getHtmlColorCode());
    }
}