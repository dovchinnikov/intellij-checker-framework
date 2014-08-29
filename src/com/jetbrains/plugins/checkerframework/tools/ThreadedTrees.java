package com.jetbrains.plugins.checkerframework.tools;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;

public class ThreadedTrees extends JavacTrees {

    public static void preRegister(Context context) {
        context.put(JavacTrees.class, new Context.Factory<JavacTrees>() {
            @Override public JavacTrees make(Context c) {
                return new ThreadedTrees(c);
            }
        });
    }

    protected ThreadedTrees(Context context) {
        super(context);
    }

    @Override public TreePath getPath(CompilationUnitTree unit, Tree node) {
        ThreadedContext.assertNotEnough();
        return super.getPath(unit, node);
    }

    @Override
    public JCTree.JCMethodDecl getTree(ExecutableElement method) {
        ThreadedContext.assertNotEnough();
        return super.getTree(method);
    }

    @Override
    public JCTree getTree(Element element) {
        ThreadedContext.assertNotEnough();
        return super.getTree(element);
    }

    @Override
    public JCTree getTree(Element e, AnnotationMirror a) {
        ThreadedContext.assertNotEnough();
        return super.getTree(e, a);
    }

    @Override
    public JCTree getTree(Element e, AnnotationMirror a, AnnotationValue v) {
        ThreadedContext.assertNotEnough();
        return super.getTree(e, a, v);
    }

    @Override
    public TreePath getPath(Element e, AnnotationMirror a, AnnotationValue v) {
        ThreadedContext.assertNotEnough();
        return super.getPath(e, a, v);
    }

    @Override
    public Symbol getElement(TreePath path) {
        ThreadedContext.assertNotEnough();
        return super.getElement(path);
    }

    @Override
    public boolean isAccessible(Scope scope, Element member, DeclaredType type) {
        ThreadedContext.assertNotEnough();
        return super.isAccessible(scope, member, type);
    }
}
