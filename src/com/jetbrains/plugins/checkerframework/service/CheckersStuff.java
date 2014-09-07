package com.jetbrains.plugins.checkerframework.service;

import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.formatter.FormatterChecker;
import org.checkerframework.checker.guieffect.GuiEffectChecker;
import org.checkerframework.checker.i18n.I18nChecker;
import org.checkerframework.checker.igj.IGJChecker;
import org.checkerframework.checker.interning.InterningChecker;
import org.checkerframework.checker.javari.JavariChecker;
import org.checkerframework.checker.linear.LinearChecker;
import org.checkerframework.checker.lock.LockChecker;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.propkey.PropertyKeyChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.signature.SignatureChecker;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.checker.units.UnitsChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.util.CheckerMain;

import java.util.Arrays;
import java.util.List;

public interface CheckersStuff {

    String PATH_TO_CHECKER = CheckerMain.findPathTo(SourceChecker.class, false);

    List<Class<? extends SourceChecker>> BUILTIN_CHECKERS = Arrays.asList(
        NullnessChecker.class,
        InterningChecker.class,
        LockChecker.class,
        FenumChecker.class,
        TaintingChecker.class,
        RegexChecker.class,
        FormatterChecker.class,
        PropertyKeyChecker.class,
        SignatureChecker.class,
        GuiEffectChecker.class,
        UnitsChecker.class,
        LinearChecker.class,
        IGJChecker.class,
        JavariChecker.class,
        SubtypingChecker.class,
        LockChecker.class,
        I18nChecker.class,
        ValueChecker.class
    );
}
