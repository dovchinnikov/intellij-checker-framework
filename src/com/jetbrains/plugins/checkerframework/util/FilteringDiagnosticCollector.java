package com.jetbrains.plugins.checkerframework.util;

import javax.tools.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class filters non-processor diagnostic messages from Java Compiler.
 */
public class FilteringDiagnosticCollector implements DiagnosticListener<JavaFileObject> {

    private static final String PROC_CODE = "compiler.err.proc.messager";

    private final List<Diagnostic<? extends JavaFileObject>> myDiagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>();

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        return myDiagnostics;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
            System.out.println(diagnostic);
        }
        if (PROC_CODE.equals(diagnostic.getCode())) {
            myDiagnostics.add(diagnostic);
        }
    }

    public List<Diagnostic<? extends JavaFileObject>> getAndClear() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = new ArrayList<Diagnostic<? extends JavaFileObject>>(myDiagnostics);
        myDiagnostics.clear();
        return diagnostics;
    }
}
