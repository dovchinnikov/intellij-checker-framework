package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiClass;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.checker.regex.qual.Regex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @author Daniil Ovchinnikov
 * @since 7/17/14
 */
@Regex
public class AwesomeInspection extends AbstractBaseJavaLocalInspectionTool {

    private final @Regex JavaCompiler myJavac = ToolProvider.getSystemJavaCompiler();
    private final StandardJavaFileManager myFileManager = myJavac.getStandardFileManager(null, null, null);
    private final Processor myProcessor = new RegexChecker();

    @Nullable
    @Override
    public ProblemDescriptor[] checkClass(@NotNull PsiClass aClass, @NotNull InspectionManager manager, boolean isOnTheFly) {
        Iterable<? extends JavaFileObject> sources = myFileManager.getJavaFileObjects(aClass.getContainingFile().getVirtualFile().getCanonicalPath());
        DiagnosticCollector<JavaFileObject> l = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler.CompilationTask task = myJavac.getTask(null, myFileManager, l, null, null, sources);
        task.setProcessors(Arrays.asList(myProcessor));
        task.call();
        for (Diagnostic d : l.getDiagnostics()) {
            System.out.println(d);
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        new AwesomeInspection().doStuff();

    }

    public void doStuff() throws IOException {
        Iterable<? extends JavaFileObject> sources = myFileManager.getJavaFileObjects(new File("src" + File.separator + AwesomeInspection.class.getCanonicalName().replace('.', File.separatorChar) + ".java").getCanonicalFile());
        DiagnosticCollector<JavaFileObject> l = new DiagnosticCollector<JavaFileObject>();
        JavaCompiler.CompilationTask task = myJavac.getTask(null, myFileManager, l, null, null, sources);

        final Processor processor = new RegexChecker();
        Processor processorProxy = (Processor) Proxy.newProxyInstance(
                Processor.class.getClassLoader(),
                new Class[]{
                        Processor.class
                },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, final Object[] args) throws Throwable {
                        if (method.getName().equals("init")) {
                            final Object processingEnvironment = args[0];
                            args[0] = Proxy.newProxyInstance(
                                    ProcessingEnvironment.class.getClassLoader(),
                                    new Class[]{ProcessingEnvironment.class},
                                    new InvocationHandler() {
                                        @Override
                                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                            final Object originalMessager = method.invoke(processingEnvironment, args);
                                            return method.getName().equals("getMessager") ? Proxy.newProxyInstance(
                                                    Messager.class.getClassLoader(),
                                                    new Class[]{Messager.class},
                                                    new InvocationHandler() {
                                                        @Override
                                                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                                            if (method.getName().equals("message")) {
                                                                System.out.println(Arrays.toString(args));
                                                            }
                                                            return method.invoke(originalMessager, args);
                                                        }
                                                    }
                                            ) : originalMessager;
                                        }
                                    }
                            );
                        }
                        try {
                            return method.invoke(processor, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    }
                }
        );

        task.setProcessors(Arrays.asList(processorProxy));
        task.call();

//        for (Diagnostic d : l.getDiagnostics()) {
//            System.out.println(d);
//        }
    }

}
