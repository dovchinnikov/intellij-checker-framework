package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.generation.surroundWith.JavaWithIfSurrounder;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.JavaSuppressionUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


public class SurroundWithIfRegexFix implements LocalQuickFix {

    private static final Logger LOG = Logger.getInstance(SurroundWithIfRegexFix.class);
    private final String myText;

    public SurroundWithIfRegexFix(@NotNull PsiExpression expressionToAssert) {
        myText = expressionToAssert.getText();
    }

    @Override
    @NotNull
    public String getName() {
        return "Surround with 'if (RegexUtil.isRegex(" + myText + "))'";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        final PsiElement element = descriptor.getPsiElement();
        final PsiFile file = element.getContainingFile();
        final Editor editor = PsiUtilBase.findEditor(element);
        if (editor == null) return;
        final Document document = editor.getDocument();
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

        final PsiStatement anchorStatement = PsiTreeUtil.getParentOfType(element, PsiStatement.class);
        final PsiElement prev = PsiTreeUtil.skipSiblingsBackward(anchorStatement, PsiWhiteSpace.class);
        final PsiElement[] elements = prev instanceof PsiComment && JavaSuppressionUtil.getSuppressedInspectionIdsIn(prev) != null
                                      ? new PsiElement[]{prev, anchorStatement}
                                      : new PsiElement[]{anchorStatement};
        try {
            TextRange textRange = new JavaWithIfSurrounder().surroundElements(project, editor, elements);
            if (textRange == null) return;

            @NonNls String newText = "org.checkerframework.checker.regex.RegexUtil.isRegex(" + myText + ")";
            document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newText);

            final PsiIfStatement ifStatement = PsiTreeUtil.getNonStrictParentOfType(
                file.findElementAt(textRange.getStartOffset()),
                PsiIfStatement.class
            );
            if (ifStatement != null) {
                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(ifStatement);
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(ifStatement);
            }

            editor.getCaretModel().moveToOffset(textRange.getStartOffset());
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        } catch (IncorrectOperationException e) {
            LOG.error(e);
        }
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return InspectionsBundle.message("inspection.surround.if.family");
    }
}
