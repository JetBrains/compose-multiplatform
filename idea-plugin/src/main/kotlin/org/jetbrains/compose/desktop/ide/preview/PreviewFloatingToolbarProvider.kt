/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponentImpl
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil

class PreviewFloatingToolbarProvider : AbstractFloatingToolbarProvider(PREVIEW_EDITOR_TOOLBAR_GROUP_ID) {
    override val autoHideable = false
    override val priority: Int = 100

    // todo: disable if not in Compose JVM module
    override fun register(toolbar: FloatingToolbarComponent, parentDisposable: Disposable) {
        try {
            // todo: use provided data context once 2020.3 is no longer supported
            val toolbarImpl = toolbar as? FloatingToolbarComponentImpl ?: return
            val editor = toolbarImpl.getData(CommonDataKeys.EDITOR.name) as? Editor ?: return
            registerComponent(toolbar, editor, parentDisposable)
        } catch (e: Exception) {
            LOG.error(e)
        }
    }

    private fun registerComponent(
        component: FloatingToolbarComponent,
        editor: Editor,
        parentDisposable: Disposable
    ) {
        val project = editor.project ?: return
        val listener = PreviewEditorToolbarVisibilityUpdater(component, project, editor)
        editor.caretModel.addCaretListener(listener, parentDisposable)
    }
}

internal class PreviewEditorToolbarVisibilityUpdater(
    private val toolbar: FloatingToolbarComponent,
    private val project: Project,
    private val editor: Editor
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        ReadAction.nonBlocking { updateVisibility() }
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun updateVisibility() {
        val parentPreviewFun = parentPreviewAtCaretOrNull(editor)
        if (parentPreviewFun != null) {
            toolbar.scheduleShow()
        } else {
            toolbar.scheduleHide()
        }
    }
}