package com.jetbrains.plugins.checkerframework.util;

import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.stubparser.JavaParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AggregateCheckerEx {

    static {
        JavaParser.setCacheParser(false);
    }

    @NotNull
    public static AggregateChecker create(final Collection<Class<? extends Processor>> classes) {
        AnnotationUtils.clear();
        return new AggregateChecker() {
            @Override
            protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
                final Set<Class<? extends SourceChecker>> myClasses = new HashSet<Class<? extends SourceChecker>>();
                for (Class<?> clazz : classes) {
                    myClasses.add(clazz.asSubclass(SourceChecker.class));
                }
                return myClasses;
            }
        };
    }

    @NotNull
    public static AggregateChecker createAndInit(final Collection<Class<? extends Processor>> classes, ProcessingEnvironment environment) {
        final AggregateChecker checker = create(classes);

        checker.init(environment);
        checker.initChecker();

        final JavacProcessingEnvironment processingEnvironment = (JavacProcessingEnvironment) environment;
        final MultiTaskListener multiTaskListener = MultiTaskListener.instance(processingEnvironment.getContext());
        for (TaskListener taskListener : multiTaskListener.getTaskListeners()) {
            multiTaskListener.remove(taskListener);
        }

        return checker;
    }
}
