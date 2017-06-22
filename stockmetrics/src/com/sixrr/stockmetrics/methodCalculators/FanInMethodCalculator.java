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
import com.sixrr.metrics.utils.BucketedCount;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.stockmetrics.utils.ProjectContainerUtil;

import java.util.*;

/**
 * @author Aleksandr Chudov.
 */
public class FanInMethodCalculator extends MethodCalculator {

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
            Set<PsiElement> methods = ProjectContainerUtil.getMethodUsers(method);
            methods.retainAll(ProjectContainerUtil.getMethods());
            //methods.remove(method);
            for (PsiMethod m : method.getContainingClass().getAllMethods()) {
                methods.remove(m);
            }
            postMetric(method, methods.size());
        }

        private boolean findInChildren(PsiElement el, PsiMethod cl) {
            if (el instanceof PsiReference) {
                PsiElement res = ((PsiReference) el).resolve();
                if (res != null && cl.equals(res)) {
                    return true;
                }
            }
            for (PsiElement e : el.getChildren()) {
                if (findInChildren(e, cl)) {
                    return true;
                }
            }
            return false;
        }
    }
}
