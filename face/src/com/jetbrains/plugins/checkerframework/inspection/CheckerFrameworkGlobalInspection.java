package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBPanel;
import com.jetbrains.plugins.checkerframework.compiler.CheckerFrameworkCompiler;
import com.jetbrains.plugins.checkerframework.compiler.CheckerFrameworkUnsharedCompiler;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkSettings;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkBundle;
import com.jetbrains.plugins.checkerframework.util.JdkVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.util.List;

public class CheckerFrameworkGlobalInspection extends GlobalSimpleInspectionTool {

    @Override
    public void initialize(@NotNull GlobalInspectionContext context) {
        if (!JdkVersion.check()) return;
        super.initialize(context);
        Project project = context.getProject();
        CheckerFrameworkSettings settings = CheckerFrameworkSettings.getInstance(project);
        context.putUserData(
            CheckerFrameworkCompiler.KEY,
            new CheckerFrameworkUnsharedCompiler(project, settings.createCompilerOptions(), settings.getEnabledCheckerClasses())
        );
    }

    @Override
    public void checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, @NotNull ProblemsHolder problemsHolder, @NotNull GlobalInspectionContext globalContext, @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor) {
        if (!JdkVersion.check()) return;
        if (!(file instanceof PsiJavaFile)) return;
        final CheckerFrameworkCompiler checkerFrameworkCompiler = globalContext.getUserData(CheckerFrameworkCompiler.KEY);
        assert checkerFrameworkCompiler != null;
        final List<ProblemDescriptor> problemDescriptors = checkerFrameworkCompiler.processFile((PsiJavaFile) file);
        if (problemDescriptors != null) {
            for (final ProblemDescriptor problemDescriptor : problemDescriptors) {
                problemDescriptionsProcessor.addProblemElement(
                    GlobalInspectionContextUtil.retrieveRefElement(problemDescriptor.getPsiElement(), globalContext),
                    problemDescriptor
                );
            }
        }
    }

    @Override
    public @Nullable JComponent createOptionsPanel() {
        if (!JdkVersion.check()) return null;
        final JBPanel panel = new JBPanel();
        final HyperlinkLabel label = new HyperlinkLabel(CheckerFrameworkBundle.message("open.settings"));
        label.addHyperlinkListener(new HyperlinkListener() {
            @Override public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    DataContext context = DataManager.getInstance().getDataContextFromFocus().getResult();
                    if (context == null) {
                        return;
                    }
                    OptionsEditor optionsEditor = OptionsEditor.KEY.getData(context);
                    if (optionsEditor == null) {
                        return;
                    }
                    Configurable configurable = optionsEditor.findConfigurableById(Stuff.CONFIGURABLE_ID);
                    if (configurable == null) {
                        return;
                    }
                    optionsEditor.clearSearchAndSelect(configurable);
                }
            }
        });
        panel.add(label);
        return panel;
    }
}

