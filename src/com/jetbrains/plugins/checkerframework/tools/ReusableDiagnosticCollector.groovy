package com.jetbrains.plugins.checkerframework.tools

import org.jetbrains.annotations.Nullable

import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

public class ReusableDiagnosticCollector implements DiagnosticListener<JavaFileObject> {

    private @Nullable DiagnosticListener<JavaFileObject> internal;

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        internal?.report(diagnostic);
    }

    public void setInternal(DiagnosticListener<JavaFileObject> internal) {
        this.internal = internal;
    }
}
