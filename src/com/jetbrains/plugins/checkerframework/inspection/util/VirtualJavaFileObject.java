package com.jetbrains.plugins.checkerframework.inspection.util;

import com.intellij.psi.PsiFile;

import javax.tools.*;
import java.io.IOException;
import java.net.URI;

public class VirtualJavaFileObject extends SimpleJavaFileObject {

    private final PsiFile myFile;

    public VirtualJavaFileObject(PsiFile file) {
        super(URI.create(file.getVirtualFile().getPath()), Kind.SOURCE);
        myFile = file;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return myFile.getText();
    }
}
