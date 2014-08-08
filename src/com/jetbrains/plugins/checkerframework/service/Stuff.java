package com.jetbrains.plugins.checkerframework.service;

public interface Stuff {

    String MANIFEST_VERSION_KEY      = "Implementation-Version";
    // TODO should be 1.8.5, set to 1.8.4 for development purposes
    String MINIMUM_SUPPORTED_VERSION = "1.8.4";
    String CHECKERS_BASE_CLASS_FQN   = "org.checkerframework.framework.source.SourceChecker";
    String CHECKERS_PACKAGE          = "org.checkerframework.checker";
    String AGGREGATE_PROCESSOR_FQN   = "com.jetbrains.plugins.checkerframework.util.AggregateCheckerEx";
    String REGEX_ANNO_FQN            = "org.checkerframework.checker.regex.qual.Regex";
    String REGEX_UTIL_FQN            = "org.checkerframework.checker.regex.RegexUtil";
}
