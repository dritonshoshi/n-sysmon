package com.nsysmon;

import com.nsysmon.impl.NSysMonImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSysMonOsDetectionTest {

    private static final String OS_NAME = "os.name";

    private String originalOsName;

    @BeforeEach
    void before() {
        originalOsName = System.getProperty(OS_NAME);
    }

    @AfterEach
    void after() {
        if (originalOsName != null) {
            System.setProperty(OS_NAME, originalOsName);
        } else {
            System.clearProperty(OS_NAME);
        }
    }

    @Test
    void isWindowsDetectsWindows() {
        System.setProperty(OS_NAME, "Windows 11");

        assertTrue(NSysMon.isWindows());
    }

    @Test
    void isWindowsIgnoresCase() {
        System.setProperty(OS_NAME, "WINDOWS SERVER 2022");

        assertTrue(NSysMon.isWindows());
    }

    @Test
    void isWindowsIsFalseOnLinux() {
        System.setProperty(OS_NAME, "Linux");

        assertFalse(NSysMon.isWindows());
    }

    @Test
    void isWindowsIsFalseOnMacOs() {
        System.setProperty(OS_NAME, "Mac OS X");

        assertFalse(NSysMon.isWindows());
    }

    @Test
    void isMacOsDetectsMacOs() {
        System.setProperty(OS_NAME, "Mac OS X");

        assertTrue(NSysMon.isMacOS());
    }

    @Test
    void isMacOsIgnoresCase() {
        System.setProperty(OS_NAME, "MAC OS X");

        assertTrue(NSysMon.isMacOS());
    }

    @Test
    void isMacOsIsFalseOnLinux() {
        System.setProperty(OS_NAME, "Linux");

        assertFalse(NSysMon.isMacOS());
    }

    @Test
    void everyFacadeMethodIsStatic() {
        assertTrue(Arrays.stream(NSysMon.class.getDeclaredMethods())
                .allMatch(m -> Modifier.isStatic(m.getModifiers())));
    }

    @Test
    void getReturnsASingletonApiInstance() {
        final NSysMonApi first = NSysMon.get();
        final NSysMonApi second = NSysMon.get();

        assertNotNull(first);
        assertSame(first, second);
    }

    @Test
    void theSingletonIsBackedByTheDefaultImplementation() {
        assertInstanceOf(NSysMonImpl.class, NSysMon.get());
    }
}
