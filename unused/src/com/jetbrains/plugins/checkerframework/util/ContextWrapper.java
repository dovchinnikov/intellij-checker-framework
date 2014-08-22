package com.jetbrains.plugins.checkerframework.util;

import com.sun.tools.javac.util.Context;

public class ContextWrapper extends Context {

    private final Context myOriginalContext;

    public ContextWrapper(Context originalContext) {
        super();
        this.myOriginalContext = originalContext;
    }

    @Override
    public <T> T get(Key<T> key) {
        final T self = super.get(key);
        return self == null ? myOriginalContext.get(key) : self;
    }

    @Override
    public <T> T get(Class<T> clazz) {
        final T self = super.get(clazz);
        return self == null ? myOriginalContext.get(clazz) : self;
    }
}
