/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diff.impl.DiffUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.editor.toolbar.floating.FloatingToolbarComponent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFileBase
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.KotlinFileType

class PreviewFloatingToolbarProvider : AbstractFloatingToolbarProvider(PREVIEW_EDITOR_TOOLBAR_GROUP_ID) {
    override val autoHideable = false

    // todo: disable if not in Compose JVM module
    override fun register(dataContext: DataContext, component: FloatingToolbarComponent, parentDisposable: Disposable) {
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        if (isInsideMainKtEditor(editor)) {
            registerComponent(component, editor, parentDisposable)
        }
    }

    private fun registerComponent(
        component: FloatingToolbarComponent,
        editor: Editor,
        parentDisposable: Disposable
    ) {
        val project = editor.project
        if (project != null && isPreviewCompatible(project)) {
            val listener = PreviewEditorToolbarVisibilityUpdater(component, project, editor)
            editor.caretModel.addCaretListener(listener, parentDisposable)
        }
    }
}

internal class PreviewEditorToolbarVisibilityUpdater(
    private val toolbar: FloatingToolbarComponent,
    private val project: Project,
    private val editor: Editor
) : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
        runNonBlocking { updateVisibility() }
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun updateVisibility() {
        if (!editor.isDisposed) {
            val parentPreviewFun = parentPreviewAtCaretOrNull(editor)
            if (parentPreviewFun != null) {
                toolbar.scheduleShow()
            } else {
                toolbar.scheduleHide()
            }
        }
    }
}

private fun isInsideMainKtEditor(editor: Editor): Boolean =
    !DiffUtil.isDiffEditor(editor) && editor.isKtFileEditor()

private fun Editor.isKtFileEditor(): Boolean {
    val documentManager = FileDocumentManager.getInstance()
    val virtualFile = documentManager.getFile(document) ?: return false
    return virtualFile !is LightVirtualFileBase
            && virtualFile.isValid
            && virtualFile.fileType == KotlinFileType.INSTANCE
}