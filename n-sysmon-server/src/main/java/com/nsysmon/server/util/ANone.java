package com.nsysmon.server.util;

/**
 * The "none" case of {@link AOption}.
 */
final class ANone<T> extends AOption<T> {
    public static final ANone<Object> INSTANCE = new ANone<>();

    private ANone() {
    }

    @Override public T get() {
        throw new IllegalStateException("ANone has no value");
    }

    @Override public boolean isDefined() {
        return false;
    }
}
