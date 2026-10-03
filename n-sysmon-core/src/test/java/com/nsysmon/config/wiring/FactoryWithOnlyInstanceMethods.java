package com.nsysmon.config.wiring;

@ABeanFactory
public class FactoryWithOnlyInstanceMethods {
    public IFixtureBean getInstance() {
        return new NoArgBean();
    }
}
