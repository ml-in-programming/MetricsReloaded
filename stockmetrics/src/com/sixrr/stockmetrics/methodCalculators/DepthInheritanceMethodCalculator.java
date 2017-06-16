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

package com.sixrr.stockmetrics.methodCalculators;

import com.intellij.ide.projectView.impl.nodes.PackageUtil;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.ui.ListUtil;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.stockmetrics.utils.ProjectContainerUtil;

import java.util.*;

import static org.bouncycastle.asn1.iana.IANAObjectIdentifiers.directory;

public class DepthInheritanceMethodCalculator extends MethodCalculator {

    @Override
    protected PsiElementVisitor createVisitor() {
        return new Visitor();
    }

    private class Visitor extends JavaRecursiveElementVisitor {

        @Override
        public void visitMethod(PsiMethod method) {
            Set<PsiPackage> packages = ProjectContainerUtil.getPackages();
            PsiClass psiClass = (PsiClass) method.getParent();
            int counter = 0;
            boolean implimentedInProject = true;
            while (ClassUtils.isConcrete(psiClass) && implimentedInProject) {
                counter++;
                PsiClass parentClass = psiClass.getSuperClass();
                implimentedInProject = false;
                if (parentClass != null) {
                    for (PsiPackage p : ClassUtils.calculatePackagesRecursive(parentClass)) {
                        PsiPackage p1 = p;
                        implimentedInProject = implimentedInProject || packages.contains(p1);
                        while (p1.getParentPackage() != null) {
                            p1 = p1.getParentPackage();
                            implimentedInProject = implimentedInProject || packages.contains(p1);
                        }
                    }
                }
                psiClass = parentClass;
            }
            postMetric(method, counter);
        }
    }
}
