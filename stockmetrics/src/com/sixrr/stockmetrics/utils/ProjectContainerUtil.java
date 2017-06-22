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
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScopes;
import com.intellij.psi.util.ClassUtil;
import com.intellij.util.containers.ContainerUtil;
import com.sixrr.metrics.utils.ClassUtils;

import java.util.*;

public class ProjectContainerUtil {

    private static Project project = null;
    private static Set<PsiPackage> packages = null;
    private static Set<PsiClass> classes = null;
    private static Set<PsiMethod> methods = null;
    private static Map<PsiElement, Set<PsiElement>> users = null;

    public static Project getProject() {
        return project;
    }

    public static Set<PsiElement> getClassUsers(PsiClass psiClass) {
        calculateUsers();
        return users.get(psiClass);
    }

    public static Set<PsiElement> getMethodUsers(PsiMethod method) {
        calculateUsers();
        return users.get(method);
    }

    private static void calculateUsers() {
        if (users == null) {
            users = new HashMap<PsiElement, Set<PsiElement>>();
            for (PsiClass c : getClasses()) {
                users.put(c, new HashSet<PsiElement>());
            }
            for (PsiMethod m : getMethods()) {
                users.put(m, new HashSet<PsiElement>());
            }
            for (PsiClass c : getClasses()) {
                for (PsiField f : c.getAllFields()) {
                    if (f.getType() instanceof PsiClassType) {
                        PsiClass type = ((PsiClassType)f.getType()).resolve();
                        if (type != null && classes.contains(type)) {
                            users.get(type).add(c);
                        }
                    }
                }
                for (PsiMethod m : c.getMethods()) {
                    if (m.getBody() != null) {
                        addInRecursion(m.getBody(), m, c);
                    }
                }
            }
        }
    }

    private static void addInRecursion(PsiElement el, PsiMethod method, PsiClass psiClass) {
        if (el instanceof PsiReference) {
            PsiElement res = ((PsiReference) el).resolve();
            if (res instanceof PsiClass && classes.contains(res)) {
                users.get(res).add(psiClass);
            }
            if (res instanceof PsiMethod && methods.contains(res)) {
                users.get(res).add(method);
            }
        }
        for (PsiElement e : el.getChildren()) {
            addInRecursion(e, method, psiClass);
        }
    }

    public static Set<PsiMethod> getMethods() {
        if (methods == null) {
            methods = new HashSet<PsiMethod>();
            for (PsiClass cl : getClasses()) {
                Collections.addAll(methods, cl.getAllMethods());
            }
        }
        return methods;
    }

    public static Set<PsiClass> getClasses() {
        if (classes == null) {
            classes = new HashSet<PsiClass>();
            for (PsiPackage p : getPackages()) {
                recursiveGetClasses(p);
            }
        }
        return classes;
    }
    private static void recursiveGetClasses(PsiPackage psiPackage) {
        Collections.addAll(classes, psiPackage.getClasses());
        for (PsiClass c : psiPackage.getClasses()) {
            recursiveGetSubClasses(c);
        }
        for (PsiPackage p : psiPackage.getSubPackages()) {
            recursiveGetClasses(p);
        }
    }

    private static void recursiveGetSubClasses(PsiClass cl) {
        classes.add(cl);
        for (PsiClass c : cl.getInnerClasses()) {
            recursiveGetSubClasses(c);
        }
    }


    public static Set<PsiPackage> getPackages() {
        if (packages == null) {
            final List<VirtualFile> sourceRoots = new ArrayList<VirtualFile>();
            final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
            ContainerUtil.addAll(sourceRoots, projectRootManager.getContentSourceRoots());

            final PsiManager psiManager = PsiManager.getInstance(project);
            packages = new HashSet<PsiPackage>();

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
                            packages.add(aPackage);
                        }
                    }
                } else {
                    // this is the case when a source root has package prefix assigned
                    packages.add(directoryPackage);
                }
            }
        }
        return packages;
    }

    public static void setProject(Project openedProject) {
        project = openedProject;
        packages = null;
        classes = null;
        methods = null;
        users = null;
    }

}
