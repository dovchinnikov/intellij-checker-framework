package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

public class PsiJavaFileObject extends SimpleJavaFileObject {

    public final @NotNull PsiJavaFile myJavaFile;

    public PsiJavaFileObject(@NotNull PsiJavaFile javaFile) throws IllegalArgumentException {
        super(URI.create(javaFile.getVirtualFile().getPath()), Kind.SOURCE);
        myJavaFile = javaFile;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return myJavaFile.getText();
    }
}
