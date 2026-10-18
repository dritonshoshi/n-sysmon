package com.nsysmon.config.wiring;

@ABeanFactory
public final class DefaultNameFactory {
    private DefaultNameFactory() {
    }

    public static IFixtureBean getInstance() {
        return new NoArgBean();
    }

    public static IFixtureBean someOtherMethod() {
        return new NoArgBean();
    }
}
