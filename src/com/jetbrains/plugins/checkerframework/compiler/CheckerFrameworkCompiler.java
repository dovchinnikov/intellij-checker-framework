package com.jetbrains.plugins.checkerframework.compiler;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiJavaFile;
import com.sun.tools.javac.api.JavacTool;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.stubparser.JavaParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardJavaFileManager;
import java.util.List;

public interface CheckerFrameworkCompiler {

    JavacTool               JAVA_COMPILER = JavacTool.create();
    StandardJavaFileManager FILE_MANAGER  = JAVA_COMPILER.getStandardFileManager(null, null, null);

    @Nullable List<ProblemDescriptor> processFile(@NotNull final PsiJavaFile psiJavaFile);

    abstract class AggregateCheckerEx extends AggregateChecker {

        static {
            JavaParser.setCacheParser(false);
        }

        @Override
        public void setProcessingEnvironment(ProcessingEnvironment env) {
            super.setProcessingEnvironment(env);
        }
    }
}
