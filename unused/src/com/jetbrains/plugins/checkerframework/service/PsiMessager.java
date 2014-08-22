package com.jetbrains.plugins.checkerframework.service;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.*;

public class PsiMessager implements Messager {

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        System.out.println(msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e) {
        System.out.println(msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
        System.out.println(msg);
    }

    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
        System.out.println(msg);
    }

    public static PsiMessager instance(Project project) {
        return ServiceManager.getService(project, PsiMessager.class);
    }
}
