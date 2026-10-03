package com.nsysmon.config.wiring;

/**
 * Fixtures used to exercise {@link HandlerForBeans}, {@link HandlerForList} and {@link HandlerForEnumsAndConstants}.
 */
interface IFixtureBean {
    String describe();
}

class NoArgBean implements IFixtureBean {
    public NoArgBean() {
    }

    @Override public String describe() {
        return "no-arg";
    }
}

class BeanWithOneStringParam implements IFixtureBean {
    final String value;

    public BeanWithOneStringParam(String value) {
        this.value = value;
    }

    @Override public String describe() {
        return value;
    }
}

class BeanWithTwoParams implements IFixtureBean {
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

class BeanWithoutMatchingCtor implements IFixtureBean {
    public BeanWithoutMatchingCtor(String a, int b) {
    }

    @Override public String describe() {
        return "never";
    }
}

class NotAFixtureBean {
    public NotAFixtureBean() {
    }
}

@ABeanFactory
class DefaultNameFactory {
    public static IFixtureBean getInstance() {
        return new NoArgBean();
    }

    public static IFixtureBean someOtherMethod() {
        return new NoArgBean();
    }
}

@ABeanFactory(factoryMethod = "create")
class CustomFactoryMethod {
    public static IFixtureBean create(String value) {
        return new BeanWithOneStringParam(value);
    }
}

@ABeanFactory
class FactoryWithIncompatibleReturnType {
    public static NotAFixtureBean getInstance() {
        return new NotAFixtureBean();
    }
}

@ABeanFactory
class FactoryWithMismatchedArity {
    public static IFixtureBean getInstance(int count) {
        return new NoArgBean();
    }
}

@ABeanFactory
class FactoryWithOnlyInstanceMethods {
    public IFixtureBean getInstance() {
        return new NoArgBean();
    }
}

enum TestEnum {
    FIRST, SECOND
}

/**
 * A non-enum type carrying public static constants, to show that the constants handler is not enum specific.
 */
class Token {
    public static final Token GOLD = new Token("gold");

    public static final String NOT_A_TOKEN = "nope";

    public final String name;

    public Token(String name) {
        this.name = name;
    }
}