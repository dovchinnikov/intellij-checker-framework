package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.plugins.checkerframework.service.CheckerFrameworkCompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author Daniil Ovchinnikov
 * @since 7/17/14
 */
public class AwesomeInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(AwesomeInspection.class);
    private static final String PROC_CODE = "compiler.err.proc.messager";

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        final List<Diagnostic<? extends JavaFileObject>> messages = CheckerFrameworkCompiler
            .getInstance(file.getProject())
            .getMessages(file);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (final Diagnostic diagnostic : messages) {
            if (!diagnostic.getCode().equals(PROC_CODE)) {
                LOG.debug("Non processor diagnostic:\n" + diagnostic);
                continue;
            }
            final String messageText = diagnostic.getMessage(Locale.getDefault());
            final @NotNull String problemKey;
            {
                int start = messageText.indexOf('[');
                int end = messageText.indexOf(']');
                if (start >= 0 && end >= start) {
                    problemKey = messageText.substring(start + 1, end);
                } else {
                    LOG.warn("problem key not found:\n" + diagnostic);
                    continue;
                }
            }
            final @Nullable PsiElement startElement = file.findElementAt((int)diagnostic.getStartPosition());
            if (startElement == null) {
                LOG.warn("Cannot find start element:\n" + diagnostic);
                continue;
            }
            final @Nullable PsiElement endElement = file.findElementAt((int)diagnostic.getEndPosition() - 1);
            if (endElement == null) {
                LOG.warn("Cannot find end element:\n" + diagnostic);
                continue;
            }
            if (startElement.getTextRange().getStartOffset() >= endElement.getTextRange().getEndOffset()) {
                LOG.warn("Wrong start & end offset: \n" + diagnostic);
                continue;
            }
            final ProblemDescriptor problemDescriptor = manager.createProblemDescriptor(
                startElement,
                endElement,
                messageText,
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                buildFixes(problemKey, startElement, endElement)
            );
            problems.add(problemDescriptor);
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }

    @SuppressWarnings("UnusedParameters")
    @Nullable
    LocalQuickFix[] buildFixes(final @NotNull String problemKey,
                               final @NotNull PsiElement startElement,
                               final @NotNull PsiElement endElement) {
        return null;
        /*
        List<LocalQuickFix> fixes = new ArrayList<LocalQuickFix>();
        if (problemKey.equals("assignment.type.incompatible")) {
            final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
            final PsiExpression enclosingExpression = PsiTreeUtil.getNonStrictParentOfType(parent,
                                                                                           PsiExpression.class);
            if (enclosingExpression != null) {
                final PsiVariable variable = PsiTreeUtil.getNonStrictParentOfType(enclosingExpression, PsiVariable.class);
                if (variable != null) {
                    fixes.add(new AddTypeCastFix(variable.getType(), enclosingExpression));
                }
            }
        } else {
            return null;
        }
        return fixes.toArray(new LocalQuickFix[fixes.size()]);
        */
    }
}
