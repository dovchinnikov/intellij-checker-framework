package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import com.jetbrains.plugins.checkerframework.util.ExpectedTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapWithAsRegexFix extends LocalQuickFixOnPsiElement {

    private final PsiClass myRegexUtilClass;

    public WrapWithAsRegexFix(PsiExpression expression, PsiClass regexUtilClass) {
        super(expression);
        myRegexUtilClass = regexUtilClass;
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
        final int numberOfRegexGroups;
        {
            final @NotNull PsiAnnotationOwner expectedTypeAnnotationOwner = ExpectedTypesUtil.findAnnotationOwner(expression);
            final @Nullable PsiAnnotation regexAnnotation = expectedTypeAnnotationOwner.findAnnotation(Stuff.REGEX_ANNO_FQN);
            if (regexAnnotation != null) {
                final PsiAnnotationMemberValue annotationMemberValue =
                    regexAnnotation.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
                assert annotationMemberValue != null;
                numberOfRegexGroups = Integer.parseInt(annotationMemberValue.getText());
            } else {
                return;
            }
        }
        final String methodCallText = "Foo.asRegex(" + expression.getText() + ", " + numberOfRegexGroups + ")";
        final PsiMethodCallExpression call = (PsiMethodCallExpression)JavaPsiFacade.getInstance(
            file.getProject()
        ).getElementFactory().createExpressionFromText(
            methodCallText, null
        );
        final PsiReferenceExpression referenceExpression =
            (PsiReferenceExpression)call.getMethodExpression().getQualifierExpression();
        assert referenceExpression != null;
        referenceExpression.bindToElement(myRegexUtilClass);
        expression.replace(call);
    }
}
