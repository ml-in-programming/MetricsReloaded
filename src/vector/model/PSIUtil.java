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

package vector.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Kivi on 16.05.2017.
 */
public class PSIUtil {
    public static Set<PsiClass> getAllSupers(PsiClass aClass, Set<PsiClass> existing) {
        Set<PsiClass> allSupers = new HashSet<PsiClass>();
        if (!existing.contains(aClass)) {
            return allSupers;
        }

        PsiClass[] supers = aClass.getSupers();
        for (PsiClass sup : supers) {
            if (!existing.contains(sup)) {
                continue;
            }
            allSupers.add(sup);
            allSupers.addAll(getAllSupers(sup, existing));
        }

        return allSupers;
    }

    public static Set<PsiClass> getAllSupers(PsiClass aClass) {
        Set<PsiClass> allSupers = new HashSet<PsiClass>();

        PsiClass[] supers = aClass.getSupers();
        for (PsiClass sup : supers) {
            allSupers.add(sup);
            allSupers.addAll(getAllSupers(sup));
        }

        return allSupers;
    }

    public static Set<PsiMethod> getAllSupers(PsiMethod method, Set<PsiClass> existing) {
        if (!existing.contains(method.getContainingClass())) {
            return new HashSet<PsiMethod>();
        }
        Set<PsiMethod> allSupers = new HashSet<PsiMethod>();

        PsiMethod[] supers = method.findSuperMethods();
        for (PsiMethod m : supers) {
            if (!existing.contains(m.getContainingClass())) {
                continue;
            }
            allSupers.add(m);
            allSupers.addAll(getAllSupers(m, existing));
        }

        return allSupers;
    }

    public static Set<PsiMethod> getAllSupers(PsiMethod method) {
        Set<PsiMethod> allSupers = new HashSet<PsiMethod>();

        PsiMethod[] supers = method.findSuperMethods();
        for (PsiMethod m : supers) {
            allSupers.add(m);
            allSupers.addAll(getAllSupers(m));
        }

        return allSupers;
    }
}
