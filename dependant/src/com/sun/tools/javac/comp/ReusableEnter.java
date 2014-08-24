package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;

public class ReusableEnter extends Enter {

    public static void preRegister(Context context) {
        context.put(enterKey, new Context.Factory<Enter>() {
            @Override
            public ReusableEnter make(Context c) {
                return new ReusableEnter(c);
            }
        });
    }

    public static Context.Key<Enter> key() {
        return enterKey;
    }

    protected ReusableEnter(Context context) {
        super(context);
    }

    public Env<AttrContext> removeEnv(Symbol.TypeSymbol sym) {
        return typeEnvs.remove(sym);
    }
}
