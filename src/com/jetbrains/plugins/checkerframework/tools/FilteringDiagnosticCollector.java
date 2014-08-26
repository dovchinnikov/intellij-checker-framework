package com.jetbrains.plugins.checkerframework.tools;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class filters non-processor diagnostic messages from Java Compiler.
 */
public class FilteringDiagnosticCollector implements DiagnosticListener<JavaFileObject> {

    private static final String PROC_CODE = "compiler.err.proc.messager";

    private final List<Diagnostic<? extends JavaFileObject>> myDiagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>();

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (PROC_CODE.equals(diagnostic.getCode()) && !diagnostic.getMessage(Locale.getDefault()).contains("EnoughException")) {
            myDiagnostics.add(diagnostic);
        }
    }

    public List<Diagnostic<? extends JavaFileObject>> getAndClear() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>(myDiagnostics);
        myDiagnostics.clear();
        return diagnostics;
    }
}
