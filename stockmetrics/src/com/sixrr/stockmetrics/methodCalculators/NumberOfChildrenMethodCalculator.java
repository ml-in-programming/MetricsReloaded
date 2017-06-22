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

import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.stockmetrics.utils.ProjectContainerUtil;
import org.junit.experimental.categories.Categories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NumberOfChildrenMethodCalculator extends MethodCalculator {

    @Override
    protected PsiElementVisitor createVisitor() {
        return new Visitor();
    }

    private class Visitor extends JavaRecursiveElementVisitor {

        @Override
        public void visitMethod(PsiMethod method) {
            if (ClassUtils.isAnonymous(method.getContainingClass())) {
                return;
            }
            Set<PsiPackage> packages = ProjectContainerUtil.getPackages();
            Set<PsiClass> classes = new HashSet<PsiClass>();
            for (PsiPackage p : packages) {
                getClasses(p, classes);
            }
            PsiClass psiClass = (PsiClass) method.getParent();
            int counter = 0;
            for (PsiClass c : classes) {
                if (c.isInheritor(psiClass, false)) {
                    counter++;
                }
            }
            postMetric(method, counter);
        }

        private void getClasses(PsiPackage p, Set<PsiClass> classes) {
            ContainerUtil.addAll(classes, p.getClasses());
            for (PsiPackage subP : p.getSubPackages()) {
                getClasses(subP, classes);
            }
        }
    }
}
