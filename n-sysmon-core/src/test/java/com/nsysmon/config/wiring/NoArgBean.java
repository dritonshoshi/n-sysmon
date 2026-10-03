package com.nsysmon.config.wiring;

public class NoArgBean implements IFixtureBean {
    public NoArgBean() {
    }

    @Override public String describe() {
        return "no-arg";
    }
}
