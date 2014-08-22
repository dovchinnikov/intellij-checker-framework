package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

public class PsiClassJavaFileObject extends SimpleJavaFileObject {

    public final @NotNull PsiClass myClass;
    public final @NotNull String   name;

    public PsiClassJavaFileObject(@NotNull PsiClass clazz) throws IllegalArgumentException {
        super(
            URI.create(clazz.getContainingFile().getVirtualFile().getPath()),
            Kind.SOURCE
        );
        if (clazz.getContainingClass() != null || clazz.getQualifiedName() == null) {
            throw new IllegalArgumentException("Illegal PSI class");
        }
        myClass = clazz;
        name = clazz.getQualifiedName();
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return myClass.getContainingFile().getText();
    }
}
