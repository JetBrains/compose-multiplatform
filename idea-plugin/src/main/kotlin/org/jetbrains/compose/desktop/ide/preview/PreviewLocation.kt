/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.KtNamedFunction

data class PreviewLocation(val fqName: String, val modulePath: String)

@RequiresReadLock
internal fun KtNamedFunction.asPreviewFunctionOrNull(): PreviewLocation? {
    if (isValidComposablePreviewFunction()) {
        val fqName = composePreviewFunctionFqn()
        val module = module?.let { ExternalSystemApiUtil.getExternalProjectPath(it) }
        if (module != null) {
            return PreviewLocation(fqName = fqName, modulePath = module)
        }
    }

    return null

}