package com.jetbrains.plugins.checkerframework.service;

public final class Stuff {

    public static final String MANIFEST_VERSION_KEY = "Implementation-Version";
    // TODO should be 1.8.5, set to 1.8.4 for development purposes
    public static final String MINIMUM_SUPPORTED_VERSION = "1.8.4";
    public static final String CHECKERS_BASE_CLASS_FQN = "org.checkerframework.framework.source.SourceChecker";
    public static final String CHECKERS_PACKAGE        = "org.checkerframework.checker";
    public static final String AGGREGATE_PROCESSOR_FQN = "com.jetbrains.plugins.checkerframework.util.AggregateCheckerEx";
    public static final String REGEX_ANNO_FQN          = "org.checkerframework.checker.regex.qual.Regex";
    public static final String REGEX_UTIL_FQN          = "org.checkerframework.checker.regex.RegexUtil";

    private Stuff() {
    }
}
