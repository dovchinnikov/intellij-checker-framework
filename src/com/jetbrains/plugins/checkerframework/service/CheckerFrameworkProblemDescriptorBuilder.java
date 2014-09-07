package com.jetbrains.plugins.checkerframework.service;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeEx;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.xml.util.XmlUtil;
import com.jetbrains.plugins.checkerframework.inspection.fix.SurroundWithIfRegexFix;
import com.jetbrains.plugins.checkerframework.inspection.fix.WrapWithAsRegexFix;
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkBundle;
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkOutputParser;
import com.jetbrains.plugins.checkerframework.util.MultiMapEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

public class CheckerFrameworkProblemDescriptorBuilder {

    private static final Logger LOG = Logger.getInstance(CheckerFrameworkProblemDescriptorBuilder.class);
    private static final MultiMapEx PROBLEM_KEY_TO_PSI_ELEMENT;

    static {
        PROBLEM_KEY_TO_PSI_ELEMENT = new MultiMapEx();
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("assignment.type.incompatible", PsiStatement.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("assignment.type.incompatible", PsiLocalVariable.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("assignment.type.incompatible", PsiField.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("argument.type.incompatible", PsiExpression.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("compound.assignment.type.incompatible", PsiStatement.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("enhancedfor.type.incompatible", PsiExpression.class);
        PROBLEM_KEY_TO_PSI_ELEMENT.putValue("return.type.incompatible", PsiStatement.class);
    }

    private final InspectionManager myInspectionManager;
    private final PsiClassType      myStringType;
    private final PsiClass          myRegexUtilClass;

    public CheckerFrameworkProblemDescriptorBuilder(final Project project) {
        myStringType = PsiType.getJavaLangString(
            PsiManager.getInstance(project),
            GlobalSearchScope.allScope(project)
        );
        myRegexUtilClass = JavaPsiFacadeEx.getInstanceEx(project).findClass(Stuff.REGEX_UTIL_FQN);
        myInspectionManager = InspectionManager.getInstance(project);
    }

    @Nullable
    public ProblemDescriptor buildProblemDescriptor(final @NotNull PsiFile file,
                                                    final @NotNull Diagnostic<? extends JavaFileObject> diagnostic,
                                                    boolean isOnTheFly) {
        return buildProblemDescriptor(
            file,
            diagnostic.getMessage(null),
            isOnTheFly
        );
    }

    @Nullable
    public ProblemDescriptor buildProblemDescriptor(final @NotNull PsiFile file,
                                                    final @NotNull String diagnosticString,
                                                    boolean isOnTheFly) {
        final CheckerFrameworkOutputParser parser = new CheckerFrameworkOutputParser(diagnosticString);
        final @NotNull String problemKey = parser.getCode();
        final int startPosition = parser.getStartPosition();
        final int endPosition = parser.getEndPosition();
        final @NotNull String description = XmlUtil.escape(StringUtil.capitalize(parser.getMessage()));

        final @Nullable PsiElement startElement = file.findElementAt(startPosition);
        if (startElement == null) {
            LOG.warn("Cannot find start element:\n" + diagnosticString);
            return null;
        }
        final @Nullable PsiElement endElement = file.findElementAt(endPosition - 1);
        if (endElement == null) {
            LOG.warn("Cannot find end element:\n" + diagnosticString);
            return null;
        }

        final @NotNull PsiElement problemElement;
        final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
        {
            final PsiElement element = PsiTreeUtil.getNonStrictParentOfType(
                parent,
                PROBLEM_KEY_TO_PSI_ELEMENT.containsKey(problemKey)
                    ? PROBLEM_KEY_TO_PSI_ELEMENT.asArray(problemKey)
                    : new Class[]{PsiElement.class}
            );
            assert element != null : "Cannot find problem element for '" + problemKey + "' key.";
            problemElement = element;
        }

        final List<LocalQuickFix> fixes = new ArrayList<>();
        final String tooltip;
        if ("assignment.type.incompatible".equals(problemKey)
            || "return.type.incompatible".equals(problemKey)
            || "argument.type.incompatible".equals(problemKey)) {
            final String foundType = XmlUtil.escape(parser.getStuff()[0]);
            final String requiredType = XmlUtil.escape(parser.getStuff()[1]);
            final PsiExpression enclosingExpression = PsiTreeUtil.getNonStrictParentOfType(
                parent,
                PsiExpression.class
            );
            if (enclosingExpression == null) {
                LOG.warn("Expression is null:\n" + diagnosticString);
                return null;
            }
            if (myStringType.equals(enclosingExpression.getType()) && requiredType.contains("@Regex")) {
                fixes.add(new WrapWithAsRegexFix(enclosingExpression, myRegexUtilClass));
                if (!"argument.type.incompatible".equals(problemKey)) {
                    fixes.add(new SurroundWithIfRegexFix(enclosingExpression, myRegexUtilClass));
                }
            }
            tooltip = CheckerFrameworkBundle.message("incompatible.types.html.tooltip", description, requiredType, foundType);
        } else {
            tooltip = description;
        }
        return myInspectionManager.createProblemDescriptor(
            problemElement,
            tooltip,
            true,
            isOnTheFly ? ProblemHighlightType.GENERIC_ERROR : ProblemHighlightType.ERROR,
            isOnTheFly,
            fixes.toArray(new LocalQuickFix[fixes.size()])
        );
    }

    @NotNull
    public static CheckerFrameworkProblemDescriptorBuilder getInstance(final @NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkProblemDescriptorBuilder.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ProblemDescriptor buildTooLongProblem(PsiFile file) {
        return myInspectionManager.createProblemDescriptor(
            file,
            CheckerFrameworkBundle.message("too.long.tooltip"),
            true,
            null,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
    }
}
