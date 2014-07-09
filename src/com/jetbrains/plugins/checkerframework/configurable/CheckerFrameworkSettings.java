package com.jetbrains.plugins.checkerframework.configurable;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@State(
    name = "CheckerFrameworkPluginSettings",
    storages = {
        @Storage(
            id = "default",
            file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkerframework-plugin-settings.xml"
        )
    }
)
public class CheckerFrameworkSettings implements PersistentStateComponent<CheckerFrameworkSettings.Settings> {

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

    private Set<String> myActiveCheckers;

    public CheckerFrameworkSettings() {
        this.myActiveCheckers = new HashSet<String>();
    }

    @Nullable
    @Override
    public Settings getState() {
        return new Settings(myActiveCheckers);
    }

    @Override
    public void loadState(Settings state) {
        myActiveCheckers.clear();
        myActiveCheckers.addAll(state.myActiveCheckers);
    }

    public Set<String> getActiveCheckers() {
        return myActiveCheckers;
    }

    public void setActiveCheckers(Set<String> activeCheckers) {
        myActiveCheckers = activeCheckers;
    }

    @NotNull
    public static CheckerFrameworkSettings getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkSettings.class);
    }

    public static class Settings {
        public Set<String> myActiveCheckers;

        public Settings() {
            myActiveCheckers = new HashSet<String>();
        }

        public Settings(Set<String> activeCheckers) {
            myActiveCheckers = activeCheckers;
        }
    }
}
