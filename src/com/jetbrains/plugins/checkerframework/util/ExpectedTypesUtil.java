package com.jetbrains.plugins.checkerframework.util;

import com.intellij.codeInsight.ExpectedTypeUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpectedTypesUtil {

    @Nullable
    public static PsiType findInAssignmentExpression(@NotNull PsiExpression expression) {
        final PsiAssignmentExpression assignmentExpression = PsiTreeUtil.getNonStrictParentOfType(
            expression,
            PsiAssignmentExpression.class
        );
        if (assignmentExpression != null) {
            assert assignmentExpression.getLExpression().getType() != null : "Assignment found but type of left expression is null";
            return assignmentExpression.getLExpression().getType();
        } else {
            return null;
        }
    }

    @Nullable
    public static PsiType findInDeclaration(@NotNull PsiExpression expression) {
        final PsiLocalVariable variable = PsiTreeUtil.getNonStrictParentOfType(expression, PsiLocalVariable.class);
        return variable != null ? variable.getType() : null;
    }

    @Nullable
    public static PsiType findInMethodCall(@NotNull PsiExpression expression) {
        final PsiMethodCallExpression methodCallExpression = PsiTreeUtil.getParentOfType(expression, PsiMethodCallExpression.class);
        if (methodCallExpression == null) return null;
        int index = -1;
        {
            final PsiExpression[] expressions = methodCallExpression.getArgumentList().getExpressions();
            for (int i = 0; i < expressions.length; i++) {
                if (expressions[i].equals(expression)) {
                    index = i;
                }
            }
        }
        if (index == -1) return null;
        final PsiMethod method = methodCallExpression.resolveMethod();
        if (method == null) return null;
        final PsiSubstitutor substitutor = ExpectedTypeUtil.inferSubstitutor(method, methodCallExpression, false);
        if (substitutor == null) return null;
        return method.getSignature(substitutor).getParameterTypes()[index];
    }

    @Nullable
    public static PsiModifierList findInReturn(@NotNull PsiExpression expression) {
        final PsiMethod method = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
        return method != null ? method.getModifierList() : null;
    }

    @NotNull
    public static PsiAnnotationOwner findAnnotationOwner(@NotNull PsiExpression expression) {
        PsiAnnotationOwner result = findInDeclaration(expression);
        if (result == null) result = findInAssignmentExpression(expression);
        if (result == null) result = findInDeclaration(expression);
        if (result == null) result = findInMethodCall(expression);
        if (result == null) result = findInReturn(expression);
        assert result != null;
        return result;
    }
}
