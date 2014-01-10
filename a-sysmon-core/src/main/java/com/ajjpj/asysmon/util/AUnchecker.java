package com.ajjpj.asysmon.util;


/**
 * This class throws an arbitrary exception without requiring it to be declared in a throws clause
 *
 * @author arno
 */
public class AUnchecker {
    public static void throwUnchecked(Throwable th) {
        AUnchecker.<RuntimeException> doIt(th);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doIt(Throwable th) throws T {
        throw (T) th;
    }

    public static void executeUnchecked(Runnable callback) {
        try {
            callback.run();
        }
        catch(Exception exc) {
            throwUnchecked (exc);
        }
    }

    public static <R> R executeUnchecked(AFunction0<R, ? extends Exception> callback) {
        try {
            return callback.apply();
        }
        catch(Exception exc) {
            throwUnchecked (exc);
            return null; //for the compiler
        }
    }
}
