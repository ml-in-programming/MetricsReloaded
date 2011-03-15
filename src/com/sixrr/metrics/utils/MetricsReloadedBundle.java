/*
 * Copyright 2005-2011 Sixth and Red River Software, Bas Leijdekkers
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

package com.sixrr.metrics.utils;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class MetricsReloadedBundle {

    private static final ResourceBundle ourBundle =
            ResourceBundle.getBundle("com.sixrr.metrics.utils.MetricsReloadedBundle");

    private MetricsReloadedBundle() {}

    public static String message(
            @PropertyKey(resourceBundle = "com.sixrr.metrics.utils.MetricsReloadedBundle") String key,
            Object... params) {
        return CommonBundle.message(ourBundle, key, params);
    }
}
