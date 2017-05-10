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

import com.intellij.codeInsight.dataflow.SetUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.sixrr.stockmetrics.utils.FieldUsageUtil;

import java.util.*;

/**
 * @author Aleksandr Chudov.
 */
public class LooseClassCouplingCalculator extends ClassCalculator {
    private final Map<PsiMethod, Set<PsiField>> methodsToFields = new HashMap<PsiMethod, Set<PsiField>>();
    private final Collection<PsiClass> visitedClasses = new ArrayList<PsiClass>();
    private final Map<PsiMethod, Set<PsiMethod>> methodsCalls = new HashMap<PsiMethod, Set<PsiMethod>>();

    @Override
    public void endMetricsRun() {
        for (final Map.Entry<PsiMethod, Set<PsiMethod>> e : methodsCalls.entrySet()) {
            if (!methodsToFields.containsKey(e.getKey())) {
                methodsToFields.put(e.getKey(), new HashSet<PsiField>());
            }
            collectFields(e.getKey(), e.getKey(), new HashSet<PsiMethod>());
        }
        for (final PsiClass aClass : visitedClasses) {
            final List<PsiMethod> methods = Arrays.asList(aClass.getMethods());
            final int n = methods.size();
            if (n < 2) {
                postMetric(aClass, 0);
                continue;
            }
            int result = 0;
            for (int i = 0; i < methods.size(); i++) {
                for (int j = i + 1; j < methods.size(); j++) {
                    final Set<PsiField> a = methodsToFields.get(methods.get(i));
                    final Set<PsiField> b = methodsToFields.get(methods.get(j));
                    if (a == null || b == null) {
                        continue;
                    }
                    if (!SetUtil.intersect(a, b).isEmpty()) {
                        result++;
                    }
                }
            }
            postMetric(aClass, result, n * (n - 1) / 2);
        }
        super.endMetricsRun();
    }

    private void collectFields(final PsiMethod current, final PsiMethod primary, final Set<PsiMethod> visited) {
        if (visited.contains(current)) {
            return;
        }
        visited.add(current);
        if (methodsToFields.containsKey(current)) {
            methodsToFields.get(primary).addAll(methodsToFields.get(current));
        }
        if (methodsCalls.get(current) == null) {
            return;
        }
        for (final PsiMethod neighbor : methodsCalls.get(current)) {
            collectFields(neighbor, current, visited);
        }
    }

    @Override
    protected PsiElementVisitor createVisitor() {
        return new Visitor();
    }

    private class Visitor extends JavaRecursiveElementVisitor {
        @Override
        public void visitClass(PsiClass aClass) {
            super.visitClass(aClass);
            if (!isConcreteClass(aClass)) {
                return;
            }
            visitedClasses.add(aClass);
            final Map<PsiField, Set<PsiMethod>> fieldToMethods = FieldUsageUtil.getFieldUsagesInMethods(executionContext, aClass);
            for (final Map.Entry<PsiField, Set<PsiMethod>> e : fieldToMethods.entrySet()) {
                for (final PsiMethod method : e.getValue()) {
                    if (!methodsToFields.containsKey(method)) {
                        methodsToFields.put(method, new HashSet<PsiField>());
                    }
                    methodsToFields.get(method).add(e.getKey());
                }
            }
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);
            final PsiMethod currentMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod.class);
            final PsiMethod calledMethod = expression.resolveMethod();
            if (currentMethod == null || currentMethod.getContainingClass() == null || calledMethod == null) {
                return;
            }
            if (!methodsCalls.containsKey(currentMethod)) {
                methodsCalls.put(currentMethod, new HashSet<PsiMethod>());
            }
            methodsCalls.get(currentMethod).add(calledMethod);
        }
    }
}
