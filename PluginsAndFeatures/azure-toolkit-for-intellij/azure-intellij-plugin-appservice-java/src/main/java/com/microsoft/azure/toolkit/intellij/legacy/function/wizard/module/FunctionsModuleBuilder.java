/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module;

import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleNameLocationSettings;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.appservice.function.AzureFunctionsUtils;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.helper.GradleFunctionsModuleBuilderHelper;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.helper.MavenFunctionsModuleBuilderHelper;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.FunctionsModuleInfoStep.GRADLE_TOOL;
import static com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.FunctionsModuleInfoStep.MAVEN_TOOL;

// todo: Refactor module builder to separate implementation of module and project, Gradle and Maven
public class FunctionsModuleBuilder extends JavaModuleBuilder {
    private WizardContext wizardContext;

    @Nullable
    @Override
    public String getBuilderId() {
        return "azurefunctions";
    }

    @Override
    public String getDescription() {
        return "Azure Functions module";
    }

    @Override
    public String getPresentableName() {
        return "Azure Functions";
    }

    @Override
    public Icon getNodeIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.FunctionApp.MODULE);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext,
                                                @NotNull final ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{new FunctionsModuleInfoStep(wizardContext)};
    }

    @Nullable
    @Override
    @ExceptionNotification
    public ModuleWizardStep modifySettingsStep(@NotNull final SettingsStep settingsStep) {
        if (settingsStep instanceof ProjectSettingsStep) {
            final ProjectSettingsStep projectSettingsStep = (ProjectSettingsStep) settingsStep;
            wizardContext.setProjectFileDirectory(Paths.get(wizardContext.getProjectFileDirectory()), false);
            projectSettingsStep.bindModuleSettings(); // workaround to update path when module name changes
            final ModuleNameLocationSettings nameLocationSettings = settingsStep.getModuleNameLocationSettings();
            final String artifactId = settingsStep.getContext().getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
            if (nameLocationSettings != null && artifactId != null) {
                nameLocationSettings.setModuleName(artifactId);
                if (!wizardContext.isCreatingNewProject()) {
                    final String parentPath = wizardContext.getUserData(AzureFunctionsConstants.PARENT_PATH);
                    nameLocationSettings.setModuleContentRoot(new File(parentPath, artifactId).getPath());
                }
            }
        }
        return super.modifySettingsStep(settingsStep);
    }

    @Override
    public ModuleWizardStep[] createFinishingSteps(@NotNull final WizardContext wizardContext,
                                                   @NotNull final ModulesProvider modulesProvider) {
        this.wizardContext = wizardContext;
        return super.createFinishingSteps(wizardContext, modulesProvider);
    }

    @Override
    @ExceptionNotification
    public void setupRootModel(@NotNull final ModifiableRootModel rootModel) throws ConfigurationException {
        final VirtualFile root = createAndGetContentEntry();
        rootModel.addContentEntry(root);
        // todo this should be moved to generic ModuleBuilder
        if (myJdk != null) {
            rootModel.setSdk(myJdk);
        } else {
            rootModel.inheritSdk();
        }
        final Project project = rootModel.getProject();
        final String tool = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_TOOL_KEY);

        if (StringUtils.equals(tool, MAVEN_TOOL)) {
            // set up maven
            MavenFunctionsModuleBuilderHelper.setupMavenModule(wizardContext, rootModel, getContentEntryPath());
        } else if (StringUtils.equals(tool, GRADLE_TOOL)) {
            GradleFunctionsModuleBuilderHelper.setupGradleModule(wizardContext, rootModel, getContentEntryPath());
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "function.create_function_module", type = AzureOperation.Type.ACTION)
    protected void setupModule(Module module) throws ConfigurationException {
        super.setupModule(module);
        final Project project = module.getProject();
        final Operation operation = TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.CREATE_FUNCTION_PROJECT);
        try {
            operation.start();
            final String tool = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_TOOL_KEY);
            final String groupId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_GROUPID_KEY);
            final String artifactId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
            final String version = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_VERSION_KEY);
            final String packageName = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_PACKAGE_NAME_KEY);
            final String[] triggers = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_TRIGGERS_KEY);
            operation.trackProperty("tool", tool);
            operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, StringUtils.join(triggers, ","));
            File tempProjectFolder = null;
            try {
                tempProjectFolder = AzureFunctionsUtils.createFunctionProjectToTempFolder(
                        groupId, artifactId, version, tool, wizardContext.isCreatingNewProject());
                if (tempProjectFolder != null) {
                    if (tempProjectFolder.exists() && tempProjectFolder.isDirectory()) {
                        final File moduleFile = new File(getContentEntryPath());
                        final File srcFolder = Paths.get(tempProjectFolder.getAbsolutePath(), "src/main/java").toFile();

                        for (final String trigger : triggers) {
                            // class name like HttpTriggerFunction
                            final String className = trigger + "Function";
                            final String fileContent = AzureFunctionsUtils.generateFunctionClassByTrigger(trigger, packageName, className);
                            final File targetFile = Paths.get(srcFolder.getAbsolutePath(), String.format("%s/%s.java",
                                    packageName.replace('.', '/'), className)).toFile();
                            targetFile.getParentFile().mkdirs();
                            FileUtils.write(targetFile, fileContent, "utf-8");
                        }
                        FileUtils.copyDirectory(tempProjectFolder, moduleFile);
                    }
                }
            } catch (final Exception e) {
                AzureMessager.getMessager().error(e, "Cannot create Azure Function Project in Java.");
                EventUtil.logError(operation, ErrorType.systemError, e, null, null);
            } finally {
                if (tempProjectFolder != null && tempProjectFolder.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(tempProjectFolder);
                    } catch (final IOException e) {
                        // ignore
                    }
                }
            }
        } finally {
            operation.complete();
        }
    }

    @Nullable
    @Override
    public ModuleWizardStep getCustomOptionsStep(final WizardContext context, final Disposable parentDisposable) {
        return new FunctionTriggerChooserStep(context);
    }

    private VirtualFile createAndGetContentEntry() {
        final String path = FileUtil.toSystemIndependentName(getContentEntryPath());
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
