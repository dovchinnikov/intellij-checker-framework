package com.jetbrains.plugins.checkerframework.configurable;


import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.openapi.project.Project;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessorProfileComboboxModel extends DefaultComboBoxModel {

    private final List<ProcessorConfigProfile> myConfigProfiles = new ArrayList<ProcessorConfigProfile>();

    public ProcessorProfileComboboxModel(Project project) {
        final CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl) CompilerConfiguration.getInstance(project);
        myConfigProfiles.add(compilerConfiguration.getDefaultProcessorProfile());
        myConfigProfiles.addAll(compilerConfiguration.getModuleProcessorProfiles());
    }

    @Override
    public ProcessorConfigProfile getElementAt(int index) {
        return myConfigProfiles.get(index);
    }

    @Override
    public int getSize() {
        return myConfigProfiles.size();
    }
}
