/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.idea

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement

fun isComposeEnabled(element: PsiElement): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
    val kotlinFacet = org.jetbrains.kotlin.idea.facet.KotlinFacet.get(module) ?: return false
    val commonArgs = kotlinFacet.configuration.settings.mergedCompilerArguments ?: return false
    val modeOption = commonArgs.pluginOptions?.firstOrNull {
        it.startsWith("plugin:androidx.compose.plugins.idea:enabled=")
    } ?: return false
    val value = modeOption.substring(modeOption.indexOf("=") + 1)
    return value == "true"
}
