/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.helper;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.intellij.util.MavenUtils;
import org.apache.commons.codec.binary.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.MavenDomModule;
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

public class MavenFunctionsModuleBuilderHelper {
    public static void setupMavenModule(final WizardContext wizardContext, @NotNull ModifiableRootModel rootModel,
                                        final String contentEntryPath) {
        final Project project = rootModel.getProject();
        final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(contentEntryPath));
        final String parentPath = wizardContext.getUserData(AzureFunctionsConstants.PARENT_PATH);
        final String groupId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_GROUPID_KEY);
        final String artifactId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
        final String version = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_VERSION_KEY);
        RefreshQueue.getInstance().refresh(true, true, () -> {
            final String packageName = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_PACKAGE_NAME_KEY);
            final MavenProject parentProject = MavenUtils.getMavenProjectByDirectory(project, parentPath);
            final MavenId mavenId = new MavenId(groupId, artifactId, version);
            final VirtualFile pomFile = Optional.ofNullable(vf).map(file -> file.findChild(MavenConstants.POM_XML)).orElse(null);
            if (pomFile == null) {
                return;
            }
            if (!wizardContext.isCreatingNewProject()) {
                updateParentProject(project, parentProject, pomFile);
            }
            final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(project);
            mavenProjectsManager.addManagedFiles(Collections.singletonList(pomFile));
        }, vf);
    }

    private static void updateParentProject(final Project project, final MavenProject parentProject, final VirtualFile pom) {
        if (parentProject == null) {
            return;
        }
        final PsiFile pomPsi = PsiManager.getInstance(project).findFile(pom);
        final PsiFile parentPomPsi = PsiManager.getInstance(project).findFile(parentProject.getFile());
        WriteCommandAction.writeCommandAction(project, new PsiFile[]{pomPsi, parentPomPsi}).run(() -> {
            final MavenDomProjectModel model = MavenDomUtil.getMavenDomProjectModel(project, pom);
            final MavenDomProjectModel parentModel = MavenDomUtil.getMavenDomProjectModel(project, parentProject.getFile());
            final String parentPacking = Optional.ofNullable(parentModel).map(pm -> pm.getPackaging().getStringValue()).orElse("unknown");
            if (StringUtils.equals(parentPacking, MavenConstants.TYPE_POM)) {
                MavenDomUtil.updateMavenParent(model, parentProject);
                final MavenDomModule parentDomModule = parentModel.getModules().addModule();
                parentDomModule.setValue(PsiManager.getInstance(project).findFile(pom));
            } else {
                AzureMessager.getMessager().info(AzureString.format("Failed to update parent pom, which packaging is {0}", parentPacking));
            }
        });
    }
}
