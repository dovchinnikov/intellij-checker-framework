package com.jetbrains.plugins.checkerframework.configurable;

import org.checkerframework.javacutil.AbstractTypeProcessor;
import org.junit.Test;

/**
 * @author Daniil Ovchinnikov.
 * @since 7/8/14.
 */
public class ClassScannerTest {

    @Test
    public void test() {
        System.out.println(ClassScanner.findChildren(AbstractTypeProcessor.class));
    }
}
