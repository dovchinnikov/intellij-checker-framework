package com.jetbrains.plugins.checkerframework.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.RemoveAnnotationQuickFix
import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import com.jetbrains.plugins.checkerframework.service.Stuff
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
public class RegexUselessPlacementInspection extends LocalInspectionTool {

    private static final Set<String> APPLICABLE_CLASSES = [
        String.class.canonicalName,
        char.canonicalName,
        Pattern.class.canonicalName,
        Matcher.class.canonicalName
    ] as Set<String>

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, final boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitAnnotation(PsiAnnotation annotation) {
                if (!Stuff.REGEX_ANNO_FQN.equals(annotation.qualifiedName)) {
                    return;
                }
                PsiType annotatedType = null;
                PsiModifierListOwner modifierListOwner = null;
                final PsiAnnotationOwner annotationOwner = annotation.owner;
                if (annotationOwner instanceof PsiModifierList) {
                    final PsiModifierList modifierList = annotationOwner as PsiModifierList;
                    if (modifierList.context instanceof PsiModifierListOwner) {
                        modifierListOwner = modifierList.context as PsiModifierListOwner;
                        if (modifierListOwner instanceof PsiVariable) {
                            annotatedType = (modifierListOwner as PsiVariable).type
                        } else if (modifierListOwner instanceof PsiMethod) {
                            annotatedType = (modifierListOwner as PsiMethod).returnType
                        } else if (modifierListOwner instanceof PsiClass) {
                            annotatedType = PsiTypesUtil.getClassType(modifierListOwner)
                        }
                        annotatedType = annotatedType?.deepComponentType;
                    } else {
                        return;
                    }
                } else if (annotationOwner instanceof PsiTypeElement) {
                    annotatedType = ((PsiTypeElement) annotationOwner).type;
                } else if (annotationOwner instanceof PsiType) {
                    annotatedType = (PsiType) annotationOwner;
                }

                if (annotatedType == null) {
                    return;
                }
                if (APPLICABLE_CLASSES.contains(annotatedType.canonicalText)) {
                    return;
                }
                holder.registerProblem(
                    holder.getManager().createProblemDescriptor(
                        annotation, "#ref doesn't make sense here",
                        true,
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        isOnTheFly,
                        new RemoveAnnotationQuickFix(annotation, modifierListOwner)
                    )
                );
            }
        };
    }
}
