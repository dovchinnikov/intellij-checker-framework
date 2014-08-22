package com.jetbrains.plugins.checkerframework.util;

import com.intellij.openapi.project.Project;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;

import javax.tools.*;
import java.util.EnumSet;
import java.util.Map;

import static com.sun.tools.javac.code.Symbol.ClassSymbol;
import static com.sun.tools.javac.code.Symbol.CompletionFailure;
import static com.sun.tools.javac.code.Symbol.PackageSymbol;

public class PsiClassReader extends ClassReader {

    private final Project myProject;
    //private final GlobalSearchScope      myProjectScope;
    private boolean initialized = false;
    private final Map<javax.lang.model.element.Name, ClassSymbol>   myProjectClassSymbolCache;
    private final Map<javax.lang.model.element.Name, PackageSymbol> myProjectPackageSymbolCache;
    //private final Map<javax.lang.model.element.Name, PackageSymbol> myProjectPackageSymbolCacheInternalHack = new HashMap<>();

    public static Context.Key<ClassReader> getKey() {
        return classReaderKey;
    }

    public PsiClassReader(Context context,
                          Project project,
                          Map<javax.lang.model.element.Name, ClassSymbol> classSymbolMap,
                          Map<javax.lang.model.element.Name, PackageSymbol> packageSymbolMap) {
        super(context, false);
        myProject = project;
        myProjectClassSymbolCache = classSymbolMap;
        myProjectPackageSymbolCache = packageSymbolMap;
        initialized = true;
        //myProjectScope = GlobalSearchScope.projectScope(project);
        //sourceCompleter = new SourceCompleter() {
        //    @Override
        //    public void complete(ClassSymbol sym) throws CompletionFailure {
        //        System.out.println(sym);
        //    }
        //};
    }

    @Override
    public ClassSymbol loadClass(Name flatname) throws CompletionFailure {
        //String interned = flatname.toString().intern();
        if (myProjectClassSymbolCache.containsKey(flatname)) {
            return myProjectClassSymbolCache.get(flatname);
        }
        try {
            ClassSymbol symbol = super.loadClass(flatname);
            myProjectClassSymbolCache.put(flatname, symbol);
            return symbol;
        } catch (CompletionFailure ex) {
            myProjectClassSymbolCache.remove(flatname);
            throw ex;
        }
    }

    @Override
    public boolean packageExists(Name fullname) {
        return super.packageExists(fullname);
    }

    @Override
    public PackageSymbol enterPackage(Name fullname) {
        if (!initialized) {
            return super.enterPackage(fullname);
        } else if (myProjectPackageSymbolCache.containsKey(fullname)) {
            return myProjectPackageSymbolCache.get(fullname);
        } else {
            PackageSymbol symbol = super.enterPackage(fullname);
            myProjectPackageSymbolCache.put(fullname, symbol);
            return symbol;
        }
    }

    @Override
    public PackageSymbol enterPackage(Name name, PackageSymbol owner) {
        return super.enterPackage(name, owner);
    }

    @Override
    protected void includeClassFile(PackageSymbol p, JavaFileObject file) {
        super.includeClassFile(p, file);
    }

    @Override
    protected JavaFileObject preferredFileObject(JavaFileObject a, JavaFileObject b) {
        return super.preferredFileObject(a, b);
    }

    @Override
    protected EnumSet<JavaFileObject.Kind> getPackageFileKinds() {
        return super.getPackageFileKinds();
    }

    @Override
    protected void extraFileActions(PackageSymbol pack, JavaFileObject fe) {
        super.extraFileActions(pack, fe);
    }
}
