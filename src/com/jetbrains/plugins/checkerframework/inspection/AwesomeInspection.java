package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.plugins.checkerframework.inspection.fix.AddTypeCastFix;
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

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull final PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        final List<Diagnostic<? extends JavaFileObject>> messages = CheckerFrameworkCompiler
            .getInstance(file.getProject())
            .getMessages(file);
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        for (Diagnostic diagnostic : messages) {
            LOG.debug(String.valueOf(diagnostic));
            final @Nullable PsiElement startElement = file.findElementAt((int)diagnostic.getStartPosition());
            if (startElement == null) {
                continue;
            }
            final @Nullable PsiElement endElement = file.findElementAt((int)diagnostic.getEndPosition() - 1);
            if (endElement == null) {
                continue;
            }
            if (startElement.getTextRange().getStartOffset() < endElement.getTextRange().getEndOffset()) {
                ProblemDescriptor problemDescriptor = null;
                final @NotNull String messageText = diagnostic.getMessage(Locale.getDefault());

                if (messageText.contains("assignment.type.incompatible")) {
                    final PsiElement parent = PsiTreeUtil.findCommonParent(startElement, endElement);
                    final PsiExpression enclosingExpression = PsiTreeUtil.getNonStrictParentOfType(parent,
                                                                                                   PsiExpression.class);
                    if (enclosingExpression == null) {
                        continue;
                    }
                    final PsiVariable variable = PsiTreeUtil.getNonStrictParentOfType(enclosingExpression, PsiVariable.class);
                    if (variable != null) {
                        problemDescriptor = manager.createProblemDescriptor(
                            startElement,
                            endElement,
                            messageText,
                            ProblemHighlightType.GENERIC_ERROR,
                            isOnTheFly,
                            new AddTypeCastFix(variable.getType(), enclosingExpression)
                        );
                    }
                }
                if (problemDescriptor == null) {
                    problemDescriptor = manager.createProblemDescriptor(
                        startElement,
                        endElement,
                        diagnostic.getMessage(Locale.getDefault()),
                        ProblemHighlightType.ERROR,
                        isOnTheFly
                    );
                }
                problems.add(problemDescriptor);
            }
        }
        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }
}
