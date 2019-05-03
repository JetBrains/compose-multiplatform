/*
 * Copyright 2018 The Android Open Source Project
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

package org.jetbrains.kotlin.r4a.idea.conversion

import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.r4a.R4aUtils

internal fun createFunctionalComponent(
    name: String,
    composeBody: String,
    imports: MutableSet<FqName>
): String {
    // TODO(jdemeulenaere): Allow to specify package.
    // We don't use org.jetbrains.kotlin.j2k.ast.Function because it requires a j2k.Converter instance to create a DeferredElement
    // (the type of the Function body).

    imports.add(R4aUtils.r4aFqName("Composable"))
    return """
            |@Composable
            |fun $name() {
            |    $composeBody
            |}""".trimMargin()
}

internal fun addR4aStarImport(targetFile: KtFile) {
    runWriteAction {
        targetFile.resolveImportReference(
            R4aUtils.r4aFqName("Composable")
        ).firstOrNull()?.let {
            ImportInsertHelper.getInstance(targetFile.project).importDescriptor(
                targetFile,
                it,
                forceAllUnderImport = true
            )
        }
    }
}