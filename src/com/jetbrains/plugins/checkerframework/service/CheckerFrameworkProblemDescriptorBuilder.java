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
import com.jetbrains.plugins.checkerframework.util.CheckerFrameworkMessages;
import com.jetbrains.plugins.checkerframework.util.MultiMapEx;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckerFrameworkProblemDescriptorBuilder {

    private static final                     Logger  LOG         = Logger.getInstance(CheckerFrameworkProblemDescriptorBuilder.class);
    private static final @Language("RegExp") String  DELIMITER   = "\\$\\$";
    private static final @Language("RegExp") String  FQN         = "[@<>\\(\\)\\.\\w\\s:]+";
    private static final                     Pattern MSG_PATTERN = Pattern.compile(
        "^\\(([\\w\\.]+)\\)\\s" + DELIMITER
        + "\\s(\\d+)\\s" + DELIMITER
        + "(" + FQN + ")" + DELIMITER
        + "(" + FQN + ")" + DELIMITER
        + "\\s\\(\\s?(\\d+)\\s?,\\s?(\\d+)\\s?\\)\\s" + DELIMITER + "([\\w\\s]+\\.)"
        + ".*$",
        Pattern.DOTALL
    );

    private static final MultiMapEx PROBLEM_KEY_TO_CLASS;

    static {
        PROBLEM_KEY_TO_CLASS = new MultiMapEx();
        PROBLEM_KEY_TO_CLASS.putValue("assignment.type.incompatible", PsiStatement.class);
        PROBLEM_KEY_TO_CLASS.putValue("assignment.type.incompatible", PsiLocalVariable.class);
        PROBLEM_KEY_TO_CLASS.putValue("argument.type.incompatible", PsiExpression.class);
        PROBLEM_KEY_TO_CLASS.putValue("compound.assignment.type.incompatible", PsiStatement.class);
        PROBLEM_KEY_TO_CLASS.putValue("enhancedfor.type.incompatible", PsiExpression.class);
        PROBLEM_KEY_TO_CLASS.putValue("return.type.incompatible", PsiStatement.class);
    }

    private final InspectionManager myInspectionManager;
    private final PsiClassType      myStringType;
    private final PsiClass          myRegexUtilClass;

    public CheckerFrameworkProblemDescriptorBuilder(final Project project) {
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        myStringType = PsiType.getJavaLangString(
            PsiManager.getInstance(project),
            searchScope
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
    public ProblemDescriptor buildProblemDescriptor(final @NotNull PsiFile file, final @NotNull String diagnosticString,
                                                    boolean isOnTheFly) {
        final Matcher matcher = MSG_PATTERN.matcher(diagnosticString);
        if (!matcher.matches()) {
            LOG.warn("Cannot parse:\n" + diagnosticString);
            return null;
        }
        final @NotNull String problemKey = matcher.group(1);
        final @SuppressWarnings("UnusedDeclaration") int magicNumber = Integer.parseInt(matcher.group(2));
        final String foundType = XmlUtil.escape(XmlUtil.escape(matcher.group(3).trim()));
        final String requiredType = XmlUtil.escape(XmlUtil.escape(matcher.group(4).trim()));
        final int startPosition = Integer.parseInt(matcher.group(5));
        final int endPosition = Integer.parseInt(matcher.group(6));
        final String description = XmlUtil.escape(StringUtil.capitalize(matcher.group(7).trim()));

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
        final List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();
        {
            final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
            {
                final PsiElement element = PsiTreeUtil.getNonStrictParentOfType(
                    parent,
                    PROBLEM_KEY_TO_CLASS.containsKey(problemKey)
                    ? PROBLEM_KEY_TO_CLASS.asArray(problemKey)
                    : new Class[]{PsiElement.class}
                );
                assert element != null : "Cannot find problem element for '" + problemKey + "' key.";
                problemElement = element;
            }
            final PsiExpression enclosingExpression = PsiTreeUtil.getNonStrictParentOfType(
                parent,
                PsiExpression.class
            );
            if (enclosingExpression == null) {
                return null;
            }
            if (myStringType.equals(enclosingExpression.getType()) && requiredType.contains("@Regex")) {
                if ("assignment.type.incompatible".equals(problemKey) || "return.type.incompatible".equals(problemKey)) {
                    fixes.add(new WrapWithAsRegexFix(enclosingExpression, myRegexUtilClass));
                    fixes.add(new SurroundWithIfRegexFix(enclosingExpression, myRegexUtilClass));
                } else if ("argument.type.incompatible".equals(problemKey)) {
                    fixes.add(new WrapWithAsRegexFix(enclosingExpression, myRegexUtilClass));
                }
            }
        }

        final String tooltip = CheckerFrameworkMessages.message("incompatible.types.html.tooltip", description, requiredType, foundType);
        return myInspectionManager.createProblemDescriptor(
            problemElement,
            tooltip,
            true,
            ProblemHighlightType.GENERIC_ERROR,
            isOnTheFly,
            fixes.toArray(new LocalQuickFix[fixes.size()])
        );
    }

    @NotNull
    public static CheckerFrameworkProblemDescriptorBuilder getInstance(final @NotNull Project project) {
        return ServiceManager.getService(project, CheckerFrameworkProblemDescriptorBuilder.class);
    }
}
