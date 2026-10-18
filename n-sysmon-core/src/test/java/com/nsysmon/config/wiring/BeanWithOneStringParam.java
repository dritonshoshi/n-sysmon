package com.nsysmon.config.wiring;

public class BeanWithOneStringParam implements IFixtureBean {
    final String value;

    public BeanWithOneStringParam(String value) {
        this.value = value;
    }

    @Override public String describe() {
        return value;
    }
}
