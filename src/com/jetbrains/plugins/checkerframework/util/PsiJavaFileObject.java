package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledFile;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

import static com.intellij.openapi.util.io.StreamUtil.loadFromStream;

public class PsiJavaFileObject extends SimpleJavaFileObject {

    public final VirtualFile myFile;
    public final String      name;
    public final Kind        kind;

    public PsiJavaFileObject(VirtualFile virtualFile) {
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
                kind = Kind.CLASS;
            } else if (rawPath.endsWith(".java")) {
                rawPath = rawPath.replace(".java", "");
                kind = Kind.SOURCE;
            } else {
                kind = Kind.OTHER;
            }
            name = rawPath.replace(File.separatorChar, '.');
        }
    }

    public PsiJavaFileObject(PsiClass clazz) {
        super(
            URI.create(clazz.getContainingFile().getVirtualFile().getPath()),
            clazz.getContainingFile() instanceof PsiCompiledFile ? Kind.CLASS : Kind.SOURCE
        );
        kind = clazz.getContainingFile() instanceof PsiCompiledFile ? Kind.CLASS : Kind.SOURCE;
        assert clazz.getContainingClass() == null;
        myFile = clazz.getContainingFile().getVirtualFile();
        name = clazz.getQualifiedName();
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return new String(loadFromStream(myFile.getInputStream()));
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return myFile.getInputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return super.openOutputStream();
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return super.openReader(ignoreEncodingErrors);
    }

    @Override
    public Writer openWriter() throws IOException {
        return super.openWriter();
    }

    @Override
    public long getLastModified() {
        return super.getLastModified();
    }

    @Override
    public boolean delete() {
        return super.delete();
    }

    @Override
    public Kind getKind() {
        return this.kind;
    }

    @Override
    public boolean isNameCompatible(String simpleName, Kind kind) {
        return super.isNameCompatible(simpleName, kind);
    }

    @Override
    public NestingKind getNestingKind() {
        return super.getNestingKind();
    }

    @Override
    public Modifier getAccessLevel() {
        return super.getAccessLevel();
    }
}
