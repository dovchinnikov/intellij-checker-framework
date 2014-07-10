package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.checkerframework.checker.compilermsgs.CompilerMessagesChecker;
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
import org.checkerframework.checker.nullness.NullnessRawnessChecker;
import org.checkerframework.checker.propkey.PropertyKeyChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.signature.SignatureChecker;
import org.checkerframework.checker.tainting.TaintingChecker;
import org.checkerframework.checker.units.UnitsChecker;
import org.checkerframework.common.subtyping.SubtypingChecker;
import org.checkerframework.framework.source.SourceChecker;

import java.util.Arrays;
import java.util.List;

@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings {

    public static final List<Class<? extends SourceChecker>> BUILTIN_CHECKERS = Arrays.asList(
        NullnessChecker.class,
        NullnessRawnessChecker.class,
        InterningChecker.class,
        LockChecker.class,
        FenumChecker.class,
        TaintingChecker.class,
        RegexChecker.class,
        FormatterChecker.class,
        PropertyKeyChecker.class,
        I18nChecker.class,
        CompilerMessagesChecker.class,
        SignatureChecker.class,
        GuiEffectChecker.class,
        UnitsChecker.class,
        LinearChecker.class,
        IGJChecker.class,
        JavariChecker.class,
        SubtypingChecker.class
    );
}
