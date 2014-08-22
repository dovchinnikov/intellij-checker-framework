package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.project.Project;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.FilteredMemberList;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Pair;

import javax.lang.model.element.*;
import java.io.Writer;
import java.util.Map;

public class PsiElements extends JavacElements {

    private final Project myProject;

    public PsiElements(Context context, Project project) {
        super(context);
        myProject = project;
    }

    @Override
    public Pair<JCTree, JCTree.JCCompilationUnit> getTreeAndTopLevel(Element e, AnnotationMirror a, AnnotationValue v) {
        return super.getTreeAndTopLevel(e, a, v);
    }

    @Override
    public boolean isFunctionalInterface(TypeElement element) {
        return super.isFunctionalInterface(element);
    }

    @Override
    public Name getName(CharSequence cs) {
        return super.getName(cs);
    }

    @Override
    public void printElements(Writer w, Element... elements) {
        super.printElements(w, elements);
    }

    @Override
    public String getConstantExpression(Object value) {
        return super.getConstantExpression(value);
    }

    @Override
    public boolean overrides(ExecutableElement riderEl, ExecutableElement rideeEl, TypeElement typeEl) {
        return super.overrides(riderEl, rideeEl, typeEl);
    }

    @Override
    public boolean hides(Element hiderEl, Element hideeEl) {
        return super.hides(hiderEl, hideeEl);
    }

    @Override
    public List<Attribute.Compound> getAllAnnotationMirrors(Element e) {
        return super.getAllAnnotationMirrors(e);
    }

    @Override
    public FilteredMemberList getAllMembers(TypeElement element) {
        return super.getAllMembers(element);
    }

    @Override
    public Map<Symbol.MethodSymbol, Attribute> getElementValuesWithDefaults(AnnotationMirror a) {
        return super.getElementValuesWithDefaults(a);
    }

    @Override
    public Name getBinaryName(TypeElement type) {
        return super.getBinaryName(type);
    }

    @Override
    public boolean isDeprecated(Element e) {
        return super.isDeprecated(e);
    }

    @Override
    public PackageElement getPackageOf(Element e) {
        return super.getPackageOf(e);
    }

    @Override
    public String getDocComment(Element e) {
        return super.getDocComment(e);
    }

    @Override
    public JCTree getTree(Element e) {
        return super.getTree(e);
    }

    @Override
    public Symbol.ClassSymbol getTypeElement(CharSequence name) {
        return super.getTypeElement(name);
    }

    @Override
    public Symbol.PackageSymbol getPackageElement(CharSequence name) {
        return super.getPackageElement(name);
    }
}
