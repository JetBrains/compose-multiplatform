/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.psi.KtNamedFunction

data class PreviewLocation(val fqName: String, val modulePath: String, val taskName: String)

@RequiresReadLock
internal fun KtNamedFunction.asPreviewFunctionOrNull(): PreviewLocation? {
    if (!isValidComposablePreviewFunction()) return null

    val fqName = composePreviewFunctionFqn()
    val module = ProjectFileIndex.getInstance(project).getModuleForFile(containingFile.virtualFile)
    if (module == null || module.isDisposed) return null

    val service = project.service<PreviewStateService>()
    val previewTaskName = service.configurePreviewTaskNameOrNull(module) ?: DEFAULT_CONFIGURE_PREVIEW_TASK_NAME
    val modulePath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
    return PreviewLocation(fqName = fqName, modulePath = modulePath, taskName = previewTaskName)
}