package com.nsysmon.config.wiring;

public class BeanWithoutMatchingCtor implements IFixtureBean {
    public BeanWithoutMatchingCtor(String a, int b) {
    }

    @Override public String describe() {
        return "never";
    }
}
