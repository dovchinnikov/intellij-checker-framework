package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.checkerframework.checker.regex.qual.Regex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUselessPlacementInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<String> APPLICABLE_CLASSES = new HashSet<String>(
        Arrays.asList(
            String.class.getCanonicalName(),
            char.class.getCanonicalName(),
            Pattern.class.getCanonicalName(),
            Matcher.class.getCanonicalName()
        )
    );

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();

        file.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitAnnotation(PsiAnnotation annotation) {
                if (!Regex.class.getCanonicalName().equals(annotation.getQualifiedName())) {
                    return;
                }
                final PsiAnnotationOwner owner = annotation.getOwner();
                PsiModifierListOwner modifierListOwner = null;
                PsiType annotatedType = null;
                if (owner instanceof PsiModifierList) {
                    final PsiModifierList modifierList = (PsiModifierList)owner;
                    modifierListOwner = (PsiModifierListOwner)modifierList.getContext();
                    if (modifierListOwner instanceof PsiVariable) {
                        annotatedType = ((PsiVariable)modifierListOwner).getType();
                    } else if (modifierListOwner instanceof PsiMethod) {
                        annotatedType = ((PsiMethod)modifierListOwner).getReturnType();
                    }
                    while (annotatedType instanceof PsiArrayType) {
                        annotatedType = ((PsiArrayType)annotatedType).getComponentType();
                    }
                } else if (owner instanceof PsiTypeElement) {
                    annotatedType = ((PsiTypeElement)owner).getType();
                } else if (owner instanceof PsiType) {
                    annotatedType = (PsiType)owner;
                }

                if (annotatedType != null) {
                    if (APPLICABLE_CLASSES.contains(annotatedType.getCanonicalText())) {
                        return;
                    }
                }
                problems.add(
                    manager.createProblemDescriptor(annotation, "#ref doesn't make sense here", true,
                                                    ProblemHighlightType.WEAK_WARNING, isOnTheFly,
                                                    new RemoveAnnotationQuickFix(annotation, modifierListOwner))
                );
            }
        });

        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
