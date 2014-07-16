package com.jetbrains.plugins.checkerframework.inspection;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.InspectionTestCase;

import java.io.File;

public class RegexUselessPlacementInspectionTest extends InspectionTestCase {

    @Override
    protected String getTestDataPath() {
        return FileUtil.toSystemIndependentName(new File("testData").getAbsolutePath());
    }

    public void testRegexUselessPlacement() throws Exception {
        doTest();
    }

    @Override
    protected Sdk getTestProjectSdk() {
        Sdk sdk = IdeaTestUtil.getMockJdk18();
        LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.JDK_1_8);
        return sdk;
    }

    private void doTest() throws Exception {
        doTest(new RegexUselessPlacementInspection());
    }

    private void doTest(final RegexUselessPlacementInspection tool) throws Exception {
        doTest("inspection/" + getTestName(true), tool);
    }
}
