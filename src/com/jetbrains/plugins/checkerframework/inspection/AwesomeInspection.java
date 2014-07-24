package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import com.sun.tools.javac.code.Scope.ImportScope;
import com.sun.tools.javac.code.Scope.StarImportScope;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.cmdline.ClasspathBootstrap;

import javax.tools.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniil Ovchinnikov
 * @since 7/17/14
 */
public class AwesomeInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final Logger LOG = Logger.getInstance(AwesomeInspection.class);
    private static final JavaCompiler JAVA_COMPILER;

    static {
        try {
            JAVA_COMPILER = (JavaCompiler)Class.forName(
                ToolProvider.getSystemJavaCompiler().getClass().getCanonicalName()
            ).newInstance();
        } catch (InstantiationException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            LOG.error(e);
            throw new RuntimeException(e);
        }
    }

    private static final StandardJavaFileManager FILE_MANAGER = JAVA_COMPILER.getStandardFileManager(null, null, null);
    private static final Collection<String> COMPILE_OPTIONS = Arrays.asList(
        "-proc:only",
        //"-version",j
        "-AprintErrorStack", "-AprintAllQualifiers",
        "-classpath",
        "/opt/checker-framework-1.8.3/checker/dist/checker-qual.jar"
        + File.pathSeparator
        + ClasspathBootstrap.getResourcePath(JAVA_COMPILER.getClass())
    );

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        //final DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        //final CompilationTask task = JAVA_COMPILER.getTask(
        //    null,
        //    FILE_MANAGER,
        //    diagnosticCollector,
        //    COMPILE_OPTIONS,
        //    null,
        //    Arrays.asList(new VirtualJavaFileObject(file))
        //);
        //task.setProcessors(Arrays.asList(new AggregateCheckerEx()));
        //task.call();
        ////
        final Collection<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
        //for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
        //    System.out.println(diagnostic);
        //    final PsiElement startElement = file.findElementAt((int)diagnostic.getStartPosition());
        //    final PsiElement endElement = file.findElementAt((int)diagnostic.getEndPosition() - 1);
        //    if (startElement != null
        //        && endElement != null
        //        && startElement.getTextRange().getStartOffset() < endElement.getTextRange().getEndOffset()) {
        //        problems.add(
        //            manager.createProblemDescriptor(
        //                startElement,
        //                endElement,
        //                diagnostic.getMessage(Locale.getDefault()),
        //                ProblemHighlightType.ERROR,
        //                isOnTheFly
        //            )
        //        );
        //    }
        //}
        //getPackageAnnotations(file);
        //Processor processor = new RegexChecker();
        //Context context = new Context();
        //DeferredDiagnosticHandler diagnosticHandler = new DeferredDiagnosticHandler(Log.instance(context));
        //JavacProcessingEnvironment environment = JavacProcessingEnvironment.instance(context);
        //
        //environment.setProcessors(Arrays.asList(processor));
        //environment.doProcessing(context,
        //                         List.nil(), // compilation units
        //                         List.nil(), // class symbols
        //                         List.nil(), // package symbols
        //                         diagnosticHandler);


        return problems.toArray(new ProblemDescriptor[problems.size()]);
    }


    private static class JCCompilationUnitEx extends JCCompilationUnit {

        protected JCCompilationUnitEx(List<JCAnnotation> packageAnnotations,
                                      JCExpression pid,
                                      List<JCTree> defs,
                                      JavaFileObject sourcefile,
                                      PackageSymbol packge,
                                      ImportScope namedImportScope,
                                      StarImportScope starImportScope) {
            super(packageAnnotations, pid, defs, sourcefile, packge, namedImportScope, starImportScope);
        }
    }

    private static List<JCTree.JCAnnotation> getPackageAnnotations(PsiFile file) {
        List<JCTree.JCAnnotation> result = List.nil();
        return result;
    }
}
