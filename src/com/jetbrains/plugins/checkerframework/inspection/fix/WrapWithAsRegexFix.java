package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class WrapWithAsRegexFix implements LocalQuickFix {

    public final PsiClass myRegexUtilClass;

    public WrapWithAsRegexFix(PsiClass regexUtilClass) {
        myRegexUtilClass = regexUtilClass;
    }

    @NotNull
    @Override
    public String getName() {
        return QuickFixBundle.message("wrap.expression.using.static.accessor.text", "RegexUtil.asRegex()");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return QuickFixBundle.message("wrap.expression.using.static.accessor.family");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final PsiElement expression = descriptor.getPsiElement();
        final PsiFile file = expression.getContainingFile();
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final String methodCallText = "Foo.asRegex(" + expression.getText() + ", 0)";
        final PsiMethodCallExpression call = (PsiMethodCallExpression)JavaPsiFacade.getInstance(
            file.getProject()
        ).getElementFactory().createExpressionFromText(
            methodCallText, null
        );
        PsiReferenceExpression referenceExpression = (PsiReferenceExpression)call.getMethodExpression().getQualifierExpression();
        assert referenceExpression != null;
        referenceExpression.bindToElement(myRegexUtilClass);
        expression.replace(call);
    }
}
