package com.jetbrains.plugins.checkerframework.inspection.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CompositeChecker extends AbstractProcessor {

    private final static Logger LOG = Logger.getInstance(CompositeChecker.class);
    private final @NotNull Collection<Processor> checkers;

    public CompositeChecker(@NotNull Collection<Class<? extends Processor>> classes) {
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
}
