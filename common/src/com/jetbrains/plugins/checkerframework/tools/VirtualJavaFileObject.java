package com.jetbrains.plugins.checkerframework.tools;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class VirtualJavaFileObject extends SimpleJavaFileObject {

    public final @NotNull VirtualFile myFile;
    public final @NotNull String      name;

    public VirtualJavaFileObject(@NotNull VirtualFile virtualFile) {
        super(URI.create(virtualFile.getPath()), Kind.CLASS);
        myFile = virtualFile;
        {
            String rawPath = virtualFile.getPath();
            int index = rawPath.indexOf('!');
            if (index > -1) {
                rawPath = rawPath.substring(index + 1);
            }
            if (rawPath.startsWith(File.separator)) {
                rawPath = rawPath.substring(File.separator.length());
            }
            if (rawPath.endsWith(".class")) {
                rawPath = rawPath.replace(".class", "");
            } else {
                throw new IllegalArgumentException("not compiled file");
            }
            name = rawPath.replace(File.separatorChar, '.');
        }
    }


    @Override
    public InputStream openInputStream() throws IOException {
        return myFile.getInputStream();
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }
}
