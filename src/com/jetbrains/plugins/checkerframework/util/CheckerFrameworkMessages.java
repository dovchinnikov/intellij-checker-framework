package com.jetbrains.plugins.checkerframework.util;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class CheckerFrameworkMessages extends AbstractBundle {

    public static final String BUNDLE = "resources.CheckerFramework";
    private static CheckerFrameworkMessages INSTANCE = new CheckerFrameworkMessages();

    private CheckerFrameworkMessages() {
        super(BUNDLE);
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
