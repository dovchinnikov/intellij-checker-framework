package com.jetbrains.plugins.checkerframework.util;

import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.stubparser.JavaParser;

import javax.annotation.processing.Processor;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
public class AggregateCheckerEx {

    static {
        JavaParser.setCacheParser(false);
    }

    public static AggregateChecker create(final Collection<Class<? extends Processor>> classes) {
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
}
