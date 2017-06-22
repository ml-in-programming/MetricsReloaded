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

package com.sixrr.stockmetrics.classCalculators;

import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.stockmetrics.utils.ProjectContainerUtil;

import java.util.HashSet;
import java.util.Set;

public class NumberOfChildrenClassCalculator extends ClassCalculator {

    @Override
    protected PsiElementVisitor createVisitor() {
        return new Visitor();
    }

    private class Visitor extends JavaRecursiveElementVisitor {

        @Override
        public void visitClass(PsiClass psiClass) {
            if (ClassUtils.isAnonymous(psiClass)) {
                return;
            }
            Set<PsiPackage> packages = ProjectContainerUtil.getPackages();
            Set<PsiClass> classes = new HashSet<PsiClass>();
            for (PsiPackage p : packages) {
                getClasses(p, classes);
            }
            int counter = 0;
            for (PsiClass c : classes) {
                if (c.isInheritor(psiClass, false)) {
                    counter++;
                }
            }
            postMetric(psiClass, counter);
        }

        private void getClasses(PsiPackage p, Set<PsiClass> classes) {
            ContainerUtil.addAll(classes, p.getClasses());
            for (PsiPackage subP : p.getSubPackages()) {
                getClasses(subP, classes);
            }
        }
    }
}
