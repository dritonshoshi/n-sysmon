package com.nsysmon.server.util;

/**
 * @author arno
 */
public abstract class AOption<T> {
    public static <T> AOption<T> some(T value) {
        return new ASome<>(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> AOption<T> none() {
        return (AOption<T>) ANone.INSTANCE;
    }

    public static <T> AOption<T> fromNullable(T value) {
        if(value == null) {
            return none();
        }
        return some(value);
    }


    public abstract T get();
    public abstract boolean isDefined();

    public boolean isEmpty() {
        return ! isDefined();
    }

    public T getOrElse(T defaultValue) {
        if(isEmpty()) {
            return defaultValue;
        }
        return get();
    }
}
