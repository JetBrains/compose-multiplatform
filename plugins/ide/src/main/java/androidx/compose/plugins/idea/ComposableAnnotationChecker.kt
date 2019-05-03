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

import androidx.compose.plugins.kotlin.ComposableAnnotationChecker
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement

class ComposableAnnotationChecker() : ComposableAnnotationChecker() {
    override fun getMode(psi: PsiElement): Mode {
        if (nullableMode != null) return nullableMode
        val module = ModuleUtilCore.findModuleForPsiElement(psi) ?: return DEFAULT_MODE
        val kotlinFacet = org.jetbrains.kotlin.idea.facet.KotlinFacet.get(module)
                ?: return DEFAULT_MODE
        val commonArgs = kotlinFacet.configuration.settings.compilerArguments ?: return DEFAULT_MODE
        val modeOption = commonArgs.pluginOptions?.firstOrNull {
            it.startsWith("plugin:androidx.compose.plugins.kotlin:syntax=")
        } ?: return DEFAULT_MODE
        return Mode.valueOf(
                modeOption.substring(modeOption.indexOf("=")+1).toUpperCase()
        )
    }
}
