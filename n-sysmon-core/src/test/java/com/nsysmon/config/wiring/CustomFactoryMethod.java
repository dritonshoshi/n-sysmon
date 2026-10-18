package com.nsysmon.config.wiring;

@ABeanFactory(factoryMethod = "create")
public final class CustomFactoryMethod {
    private CustomFactoryMethod() {
    }

    public static IFixtureBean create(String value) {
        return new BeanWithOneStringParam(value);
    }
}
