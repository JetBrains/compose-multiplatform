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

package androidx.compose.plugins.idea.conversion

import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import androidx.compose.plugins.kotlin.ComposeUtils

internal fun createFunctionalComponent(
    name: String,
    composeBody: String,
    imports: MutableSet<FqName>
): String {
    // TODO(jdemeulenaere): Allow to specify package.
    // We don't use org.jetbrains.kotlin.j2k.ast.Function because it requires a j2k.Converter instance to create a DeferredElement
    // (the type of the Function body).

    imports.add(ComposeUtils.composeFqName("Composable"))
    return """
            |@Composable
            |fun $name() {
            |    $composeBody
            |}""".trimMargin()
}

internal fun addComposeStarImport(targetFile: KtFile) {
    runWriteAction {
        targetFile.resolveImportReference(
            ComposeUtils.composeFqName("Composable")
        ).firstOrNull()?.let {
            ImportInsertHelper.getInstance(targetFile.project).importDescriptor(
                targetFile,
                it,
                forceAllUnderImport = true
            )
        }
    }
}