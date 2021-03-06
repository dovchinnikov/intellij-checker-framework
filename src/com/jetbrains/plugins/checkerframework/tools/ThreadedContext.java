package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.openapi.progress.ProgressManager;
import com.sun.tools.javac.util.Context;

public class ThreadedContext extends Context {

    public static void assertNotEnough() {
        ProgressManager.checkCanceled();
    }

    @Override
    public <T> void put(Key<T> key, Factory<T> fac) {
        assertNotEnough();
        super.put(key, fac);
    }

    @Override
    public <T> void put(Key<T> key, T data) {
        assertNotEnough();
        super.put(key, data);
    }

    @Override
    public <T> T get(Key<T> key) {
        assertNotEnough();
        return super.get(key);
    }

    @Override
    public <T> T get(Class<T> clazz) {
        assertNotEnough();
        return super.get(clazz);
    }

    @Override
    public <T> void put(Class<T> clazz, T data) {
        assertNotEnough();
        super.put(clazz, data);
    }

    @Override
    public <T> void put(Class<T> clazz, Factory<T> fac) {
        assertNotEnough();
        super.put(clazz, fac);
    }
}
