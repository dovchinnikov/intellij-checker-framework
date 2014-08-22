package com.jetbrains.plugins.checkerframework.util;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;

public class PsiSymtab extends Symtab {

    public static Context.Key<Symtab> getKey() {
        return symtabKey;
    }

    /**
     * Constructor; enters all predefined identifiers and operators
     * into symbol table.
     *
     * @param context
     */
    protected PsiSymtab(Context context) throws Symbol.CompletionFailure {
        super(context);
    }
}
