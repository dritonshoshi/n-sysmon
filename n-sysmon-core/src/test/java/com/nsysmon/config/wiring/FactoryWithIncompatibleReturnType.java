package com.nsysmon.config.wiring;

@ABeanFactory
public final class FactoryWithIncompatibleReturnType {
    private FactoryWithIncompatibleReturnType() {
    }

    public static NotAFixtureBean getInstance() {
        return new NotAFixtureBean();
    }
}
