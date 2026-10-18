package com.nsysmon.config.wiring;

/**
 * A non-enum type carrying public static constants, to show that the constants handler is not enum specific.
 */
public class Token {
    public static final Token GOLD = new Token("gold");

    public static final String NOT_A_TOKEN = "nope";

    public final String name;

    public Token(String name) {
        this.name = name;
    }
}
