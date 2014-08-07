package com.jetbrains.plugins.checkerframework.util;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;

import java.util.Collection;

public class MultiMapEx extends MultiMap<String, Class<? extends PsiElement>> {

    @SuppressWarnings("unchecked")
    public Class[] asArray(String key) {
        final Collection<Class<? extends PsiElement>> result = get(key);
        return result.toArray(new Class[result.size()]);
    }
}
