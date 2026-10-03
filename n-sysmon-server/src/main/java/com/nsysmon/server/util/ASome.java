package com.nsysmon.server.util;

/**
 * The "some" case of {@link AOption}.
 */
final class ASome<T> extends AOption<T> {
    private final T value;

    ASome(T value) {
        this.value = value;
    }

    @Override public T get() {
        return value;
    }

    @Override public boolean isDefined() {
        return true;
    }
}
