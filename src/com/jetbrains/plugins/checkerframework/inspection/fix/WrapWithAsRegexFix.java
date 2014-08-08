package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class WrapWithAsRegexFix extends BaseRegexFix {

    public WrapWithAsRegexFix(PsiExpression expression, PsiClass regexUtilClass) {
        super(expression, regexUtilClass);
    }

    @NotNull
    @Override
    public String getText() {
        return QuickFixBundle.message("wrap.expression.using.static.accessor.text", "RegexUtil.asRegex()");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return QuickFixBundle.message("wrap.expression.using.static.accessor.family");
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement ignored) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final PsiExpression expression = (PsiExpression)startElement;
        final int numberOfRegexGroups = getNumberOfExpectedRegexGroups(expression);
        final String methodCallText = "Foo.asRegex(" + expression.getText() + ", " + numberOfRegexGroups + ")";
        final PsiMethodCallExpression call = (PsiMethodCallExpression)JavaPsiFacade.getInstance(
            project
        ).getElementFactory().createExpressionFromText(
            methodCallText, null
        );
        final PsiReferenceExpression referenceExpression = (PsiReferenceExpression)call.getMethodExpression().getQualifierExpression();
        assert referenceExpression != null;
        referenceExpression.bindToElement(myRegexUtilClass);
        expression.replace(call);
    }
}
