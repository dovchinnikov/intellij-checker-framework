package com.sun.tools.javac.api;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import javax.tools.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.sun.tools.javac.api.JavacTool.processOptions;

public class PsiJavacTool {

    public static JavacTask getTask(Iterable<String> options, Context context, Iterable<? extends JavaFileObject> compilationUnits) {
        try {
            final ClientCodeWrapper ccw = ClientCodeWrapper.instance(context);
            final JavaFileManager fileManager = context.get(JavaFileManager.class);
            processOptions(context, fileManager, options);
            final Main compiler = new Main("javacTask", context.get(Log.outKey));
            final Constructor<JavacTaskImpl> constructor = JavacTaskImpl.class.getDeclaredConstructor(
                Main.class, Iterable.class, Context.class, Iterable.class, Iterable.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(compiler, options, context, null, ccw.wrapJavaFileObjects(compilationUnits));
        } catch (ClientCodeException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
