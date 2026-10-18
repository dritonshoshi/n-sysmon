package com.nsysmon.config.wiring;

@ABeanFactory
public final class FactoryWithMismatchedArity {
    private FactoryWithMismatchedArity() {
    }

    public static IFixtureBean getInstance(int count) {
        return new NoArgBean();
    }
}
