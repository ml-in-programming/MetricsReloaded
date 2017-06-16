/*
 * Copyright 2005-2017 Sixth and Red River Software, Bas Leijdekkers
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.sixrr.stockmetrics.utils;

import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.impl.nodes.PackageUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.util.containers.ContainerUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectContainerUtil {

    private static Project project;

    public static Project getProject() {
        return project;
    }

    public static Set<PsiPackage> getPackages() {
        final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
        final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());

        final PsiManager psiManager = PsiManager.getInstance(project);
        final Set<PsiPackage> topLevelPackages = new HashSet<PsiPackage>();

        for (final VirtualFile root : sourceRoots) {
            final PsiDirectory directory = psiManager.findDirectory(root);
            if (directory == null) {
                continue;
            }
            final PsiPackage directoryPackage = JavaDirectoryService.getInstance().getPackage(directory);
            if (directoryPackage == null || PackageUtil.isPackageDefault(directoryPackage)) {
                final PsiDirectory[] subdirectories = directory.getSubdirectories();
                for (PsiDirectory subdirectory : subdirectories) {
                    final PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(subdirectory);
                    if (aPackage != null && !PackageUtil.isPackageDefault(aPackage)) {
                        topLevelPackages.add(aPackage);
                    }
                }
            } else {
                // this is the case when a source root has package prefix assigned
                topLevelPackages.add(directoryPackage);
            }
        }

        return topLevelPackages;
    }

    public static void setProject(Project openedProject) {
        project = openedProject;
    }

}
