package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.diagnostic.Logger;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CompositeChecker extends AbstractProcessor {

    private final static Logger LOG = Logger.getInstance(CompositeChecker.class);
    private final @NotNull Collection<Processor> checkers;

    public CompositeChecker(@NotNull Collection<Class<? extends Processor>> classes, @NotNull JavacTask task) {
        checkers = new HashSet<Processor>();
        for (Class<? extends Processor> clazz : classes) {
            try {
                checkers.add(clazz.newInstance());
            } catch (InstantiationException e) {
                LOG.error(e);
            } catch (IllegalAccessException e) {
                LOG.error(e);
            }
        }
        task.addTaskListener(
            new TaskListener() {
                @Override
                public void started(TaskEvent e) {
                }

                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        for (Processor checker : checkers) {
                            final Field hackField = getFieldInHierarchy(checker.getClass(), "errsOnLastExit");
                            if (hackField == null) {
                                continue;
                            }
                            hackField.setAccessible(true);
                            try {
                                hackField.setInt(checker, Integer.MAX_VALUE - 1);
                            } catch (IllegalAccessException ignored) {
                            }
                        }
                    }
                }
            }
        );
    }

    @Override
    public Set<String> getSupportedOptions() {
        final Set<String> result = new HashSet<String>();
        for (Processor checker : checkers) {
            result.addAll(checker.getSupportedOptions());
        }
        return result;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> result = new HashSet<String>();
        for (Processor checker : checkers) {
            result.addAll(checker.getSupportedAnnotationTypes());
        }
        return result;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        for (Processor checker : checkers) {
            checker.init(processingEnv);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Processor checker : checkers) {
            checker.process(annotations, roundEnv);
        }
        return false;
    }

    @Nullable
    public static Field getFieldInHierarchy(@NotNull Class<?> clazz, @NotNull String fieldName) {
        Field hackField = null;
        Class<?> superClazz = clazz;
        while (hackField == null && superClazz != null) {
            try {
                hackField = superClazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                superClazz = superClazz.getSuperclass();
            }
        }
        return hackField;
    }

    @Nullable
    public static Method getMethodInHierarchy(@NotNull Class<?> clazz, @NotNull String methodName, @NotNull Class... args) {
        Method method = null;
        Class<?> superClazz = clazz;
        while (method == null && superClazz != null) {
            try {
                method = superClazz.getDeclaredMethod(methodName, args);
            } catch (NoSuchMethodException e) {
                superClazz = superClazz.getSuperclass();
            }
        }
        return method;
    }
}
