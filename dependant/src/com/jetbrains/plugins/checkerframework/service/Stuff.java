package com.jetbrains.plugins.checkerframework.service;

import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.lock.LockChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.units.UnitsChecker;
import org.checkerframework.framework.source.SourceChecker;

import java.util.Arrays;
import java.util.List;

public interface Stuff {

//    String MANIFEST_VERSION_KEY      = "Implementation-Version";
    // TODO should be 1.8.5, set to 1.8.4 for development purposes
//    String MINIMUM_SUPPORTED_VERSION = "1.8.4";
//    String CHECKERS_BASE_CLASS_FQN   = "org.checkerframework.framework.source.SourceChecker";
//    String CHECKERS_PACKAGE          = "org.checkerframework.checker";
//    String AGGREGATE_PROCESSOR_FQN   = "com.jetbrains.plugins.checkerframework.util.AggregateCheckerEx";
//    String COMPILER_IMPL_FQN         = "com.jetbrains.plugins.checkerframework.util.CheckerFrameworkCompilerImpl";
    String REGEX_ANNO_FQN            = "org.checkerframework.checker.regex.qual.Regex";
    String REGEX_UTIL_FQN            = "org.checkerframework.checker.regex.RegexUtil";

    List<Class<? extends SourceChecker>> BUILTIN_CHECKERS = Arrays.asList(
        NullnessChecker.class,
        FenumChecker.class,
        RegexChecker.class,
        UnitsChecker.class,
        LockChecker.class,
        I18nChecker.class
    );
}
