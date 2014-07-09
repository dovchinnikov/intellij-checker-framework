package com.jetbrains.plugins.checkerframework.configurable;

import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.junit.Test;

public class ClassScannerTest {

    @Test
    public void test() {
        System.out.println(ClassScanner.findChildren(AbstractTypeProcessor.class, null));
    }
}
