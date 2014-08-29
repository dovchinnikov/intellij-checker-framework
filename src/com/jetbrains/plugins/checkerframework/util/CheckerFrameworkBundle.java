package com.jetbrains.plugins.checkerframework.util;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class CheckerFrameworkBundle extends AbstractBundle {

    public static final String                 BUNDLE   = "resources.CheckerFramework";
    private static      CheckerFrameworkBundle INSTANCE = new CheckerFrameworkBundle();

    private CheckerFrameworkBundle() {
        super(BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
