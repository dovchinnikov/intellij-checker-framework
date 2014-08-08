package com.jetbrains.plugins.checkerframework.inspection.fix;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.generation.surroundWith.JavaWithIfSurrounder;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.JavaSuppressionUtil;
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


public class SurroundWithIfRegexFix extends BaseRegexFix {

    private static final Logger LOG = Logger.getInstance(SurroundWithIfRegexFix.class);

    public SurroundWithIfRegexFix(@NotNull PsiElement element, @NotNull PsiClass aClass) {
        super(element, aClass);
    }

    @NotNull
    @Override
    public String getText() {
        return "Surround with 'if (RegexUtil.isRegex()'";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        final PsiExpression expression = (PsiExpression)startElement;
        final int numberOfRegexGroups = getNumberOfExpectedRegexGroups(expression);
        final Editor editor = PsiUtilBase.findEditor(expression);
        if (editor == null) return;
        final Document document = editor.getDocument();
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return;

        final PsiStatement anchorStatement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class);
        final PsiElement prev = PsiTreeUtil.skipSiblingsBackward(anchorStatement, PsiWhiteSpace.class);
        final PsiElement[] elements = prev instanceof PsiComment && JavaSuppressionUtil.getSuppressedInspectionIdsIn(prev) != null
                                      ? new PsiElement[]{prev, anchorStatement}
                                      : new PsiElement[]{anchorStatement};
        try {
            final TextRange textRange = new JavaWithIfSurrounder().surroundElements(project, editor, elements);
            if (textRange == null) return;
            final @NonNls String methodCallText = "org.checkerframework.checker.regex.RegexUtil.isRegex("
                                                  + expression.getText()
                                                  + ", "
                                                  + numberOfRegexGroups
                                                  + ")";
            document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), methodCallText);

            final PsiIfStatement ifStatement = PsiTreeUtil.getNonStrictParentOfType(
                file.findElementAt(textRange.getStartOffset()),
                PsiIfStatement.class
            );
            assert ifStatement != null;
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(ifStatement);
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(ifStatement);

            editor.getCaretModel().moveToOffset(textRange.getStartOffset() + methodCallText.length());
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
