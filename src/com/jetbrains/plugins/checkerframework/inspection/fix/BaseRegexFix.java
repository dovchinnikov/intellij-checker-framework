package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.psi.*;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import com.jetbrains.plugins.checkerframework.util.ExpectedTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseRegexFix extends LocalQuickFixOnPsiElement {

    protected final PsiClass myRegexUtilClass;

    public BaseRegexFix(@NotNull PsiElement element, @NotNull PsiClass aClass) {
        super(element);
        myRegexUtilClass = aClass;
    }

    protected static int getNumberOfExpectedRegexGroups(final @NotNull PsiExpression expression) {
        final @NotNull PsiAnnotationOwner expectedTypeAnnotationOwner = ExpectedTypesUtil.findAnnotationOwner(expression);
        final @Nullable PsiAnnotation regexAnnotation = expectedTypeAnnotationOwner.findAnnotation(Stuff.REGEX_ANNO_FQN);
        assert regexAnnotation != null : "@Regex was not found";
        final PsiAnnotationMemberValue annotationMemberValue =
            regexAnnotation.findAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
        assert annotationMemberValue != null : "Should never throw this";
        return Integer.parseInt(annotationMemberValue.getText());
    }
}
