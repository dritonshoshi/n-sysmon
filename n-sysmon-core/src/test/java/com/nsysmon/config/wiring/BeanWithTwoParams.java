package com.nsysmon.config.wiring;

public class BeanWithTwoParams implements IFixtureBean {
    final String name;
    final int count;

    public BeanWithTwoParams(String name, int count) {
        this.name = name;
        this.count = count;
    }

    @Override public String describe() {
        return name + "=" + count;
    }
}
