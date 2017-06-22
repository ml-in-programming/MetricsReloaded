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

import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.sixrr.metrics.Metric;
import com.sixrr.metrics.MetricsExecutionContext;
import com.sixrr.metrics.MetricsResultsHolder;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.stockmetrics.utils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * @author Aleksandr Chudov.
 */
public class FanOutClassCalculator extends ClassCalculator {



    @Override
    protected PsiElementVisitor createVisitor() {
        return new Visitor();
    }

    private class Visitor extends JavaRecursiveElementVisitor {
        @Override
        public void visitClass(PsiClass c) {
            if (ClassUtils.isAnonymous(c)) {
                return;
            }
            Set<PsiClass> classes = new HashSet<PsiClass>();
            for (PsiField f : c.getAllFields()) {
                if (f.getType() instanceof PsiClassType) {
                    PsiClass type = ((PsiClassType)f.getType()).resolve();
                    if (type != null) {
                        classes.add(type);
                    }
                }
            }
            for (PsiMethod m : c.getAllMethods()) {
                if (m.getBody() != null) {
                    for (PsiStatement s : m.getBody().getStatements()) {
                        classes.addAll(findInChildren(s));
                    }
                }
            }
            classes.retainAll(ProjectContainerUtil.getClasses());
            classes.remove(c);
            postMetric(c, classes.size());
        }

        private Set<PsiClass> findInChildren(PsiElement el) {
            HashSet<PsiClass> classes = new HashSet<PsiClass>();
            if (el instanceof PsiReference) {
                PsiElement res = ((PsiReference) el).resolve();
                if (res != null && res instanceof PsiMethod) {
                    classes.add(((PsiMethod) res).getContainingClass());
                }
            }
            for (PsiElement e : el.getChildren()) {
                classes.addAll(findInChildren(e));
            }
            return classes;
        }
    }/**/
        /*@Override
        public void visitClass(PsiClass aClass) {
            if (ClassUtils.isAnonymous(aClass)) {
                return;
            }
            Set<PsiElement> classes = ProjectContainerUtil.getClassUsers(aClass);
            classes.retainAll(ProjectContainerUtil.getClasses());
            classes.remove(aClass);
            postMetric(aClass, classes.size());
        }
    }/**/
}
