package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NonStaticFenumDeclaration extends LocalInspectionTool {

    private final Map<String, PsiType> myNameTypeMap = new HashMap<String, PsiType>();
    private final SuppressManager mySuppressManager = SuppressManager.getInstance();

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        final PsiJavaFile javaFile = (PsiJavaFile)file;

        javaFile.accept(new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitField(PsiField field) {
                final PsiAnnotation annotation = AnnotationUtil.findAnnotation(field, Fenum.class.getCanonicalName());
                if (annotation == null) {
                    return;
                }
                final String fakeEnumName;
                {
                    final PsiAnnotationMemberValue annotationMemberValue = annotation.findDeclaredAttributeValue("value");
                    if (annotationMemberValue instanceof PsiLiteralExpression) {
                        final Object valueObject = ((PsiLiteralExpression)annotationMemberValue).getValue();
                        if (valueObject == null) {
                            return;
                        } else {
                            fakeEnumName = valueObject.toString();
                        }
                    } else {
                        return;
                    }
                }
                if (mySuppressManager.isSuppressedFor(field, "fenum:assignment.type.incompatible")) {
                    if (myNameTypeMap.containsKey(fakeEnumName)) {
                        if (!myNameTypeMap.get(fakeEnumName).equals(field.getType())) {
                            System.out.println("type problem");
                        }
                    } else {
                        myNameTypeMap.put(fakeEnumName, field.getType());
                    }
                    if (!field.hasModifierProperty(PsiModifier.STATIC)) {
                        System.out.println("non-static: " + fakeEnumName);
                    }
                    if (!field.hasModifierProperty(PsiModifier.FINAL)) {
                        System.out.println("non-final: " + fakeEnumName);
                    }
                } else {
                    if (!myNameTypeMap.containsKey(fakeEnumName)) {
                        System.out.println("not declared problem: " + fakeEnumName);
                    }
                }
            }
        });

        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    private static boolean containsDeclarations(final PsiJavaFile file) {
        for (final PsiClass clazz : file.getClasses()) {
            if (clazz.getModifierList() == null) {
                continue;
            }
            for (final PsiAnnotation annotation : clazz.getModifierList().getAnnotations()) {
                if (SuppressWarnings.class.getCanonicalName().equals(annotation.getQualifiedName())) {
                    final PsiAnnotationParameterList parameters = annotation.getParameterList();
                    for (final PsiNameValuePair nameValuePair : parameters.getAttributes()) {
                        final PsiAnnotationMemberValue value = nameValuePair.getValue();
                        if (value == null) {
                            continue;
                        }
                        if ("fenum:assignment.type.incompatible".equals(value.getText())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

