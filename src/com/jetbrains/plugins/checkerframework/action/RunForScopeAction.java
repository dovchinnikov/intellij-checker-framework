package com.jetbrains.plugins.checkerframework.action;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.actions.RunInspectionIntention;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.inspection.AwesomeInspection;
import com.jetbrains.plugins.checkerframework.service.CompilerHolder;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComponentNotRegistered")
public abstract class RunForScopeAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        {   // reset compiler (symtab, processor caches, etc)
            final Project project = e.getProject();
            assert project != null;
            CompilerHolder.getInstance(project).resetSync();
        }
        final InspectionToolWrapper toolWrapper = new LocalInspectionToolWrapper(new AwesomeInspection());
        final InspectionManagerEx inspectionManagerEx = (InspectionManagerEx) InspectionManager.getInstance(e.getProject());
        final AnalysisScope scope = getScope(e);
        RunInspectionIntention.rerunInspection(toolWrapper, inspectionManagerEx, scope, null);
    }

    public abstract @NotNull AnalysisScope getScope(AnActionEvent e);

    public static class RunForCurrentFile extends RunForScopeAction {

        @Override
        public @NotNull AnalysisScope getScope(AnActionEvent e) {
            final PsiFile currentFile = e.getData(CommonDataKeys.PSI_FILE);
            assert currentFile != null;
            return new AnalysisScope(currentFile);
        }

        @Override
        public void update(AnActionEvent e) {
            final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
            e.getPresentation().setEnabled(file != null && file.getLanguage().is(JavaLanguage.INSTANCE));
        }
    }

    public static class RunForCurrentModule extends RunForScopeAction {

        @Override
        public @NotNull AnalysisScope getScope(AnActionEvent e) {
            final VirtualFile vf = e.getData(CommonDataKeys.VIRTUAL_FILE);
            final Project project = e.getProject();
            assert vf != null && project != null;
            final Module module = ModuleUtilCore.findModuleForFile(vf, project);
            assert module != null;
            return new AnalysisScope(module);
        }
    }

    public static class RunForCurrentProject extends RunForScopeAction {

        @Override
        public @NotNull AnalysisScope getScope(AnActionEvent e) {
            assert e.getProject() != null;
            return new AnalysisScope(e.getProject());
        }
    }
}
