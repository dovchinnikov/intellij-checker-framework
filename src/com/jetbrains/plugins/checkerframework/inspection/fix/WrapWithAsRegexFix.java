package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.plugins.checkerframework.service.Stuff;
import org.jetbrains.annotations.NotNull;

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
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement expression, @NotNull PsiElement ignored) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        final @NotNull PsiAnnotationOwner requiredTypePsi;
        final int numberOfRegexGroups;
        {
            PsiLocalVariable variable = PsiTreeUtil.getNonStrictParentOfType(expression, PsiLocalVariable.class);
            if (variable != null) {
                requiredTypePsi = variable.getType();
            } else {
                PsiAssignmentExpression assignmentExpression = PsiTreeUtil.getNonStrictParentOfType(
                    expression,
                    PsiAssignmentExpression.class
                );
                if (assignmentExpression != null) {
                    assert assignmentExpression.getLExpression().getType() != null : "Assignment left expression type is null";
                    requiredTypePsi = assignmentExpression.getLExpression().getType();
                } else {
                    assert expression.getContext() instanceof PsiReturnStatement;
                    PsiMethod method = PsiTreeUtil.getNonStrictParentOfType(expression, PsiMethod.class);
                    assert method != null : "Cannot find type";
                    requiredTypePsi = method.getModifierList();
                }
            }
        }
        {
            final PsiAnnotation regexAnnotation = requiredTypePsi.findAnnotation(Stuff.REGEX_ANNO_FQN);
            if (regexAnnotation != null) {
                final PsiAnnotationMemberValue annotationMemberValue = regexAnnotation.findAttributeValue("value");
                if (annotationMemberValue != null) {
                    numberOfRegexGroups = Integer.parseInt(annotationMemberValue.getText());
                } else {
                    return;
                }
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
