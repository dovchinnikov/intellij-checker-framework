package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class VirtualJavaFileObject extends SimpleJavaFileObject {

    public final @NotNull VirtualFile myFile;

    public VirtualJavaFileObject(@NotNull VirtualFile virtualFile) {
        super(URI.create(virtualFile.getPath()), Kind.CLASS);
        myFile = virtualFile;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return myFile.getInputStream();
    }
}
