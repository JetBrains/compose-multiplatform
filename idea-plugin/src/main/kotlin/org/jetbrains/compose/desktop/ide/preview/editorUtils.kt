/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtNamedFunction

@RequiresReadLock
internal fun parentPreviewAtCaretOrNull(editor: Editor): PreviewLocation? {
    val caretModel = editor.caretModel
    val psiFile = kotlinPsiFile(editor)
    if (psiFile != null) {
        var node = psiFile.findElementAt(caretModel.offset)
        while (node != null) {
            val previewFunction = (node as? KtNamedFunction)?.asPreviewFunctionOrNull()
            if (previewFunction != null) {
                return previewFunction
            }
            node = node.parent
        }
    }

    return null
}

private fun kotlinPsiFile(editor: Editor): PsiFile? {
    val project = editor.project ?: return null
    val documentManager = FileDocumentManager.getInstance()
    val file = documentManager.getFile(editor.document)
    return if (file != null && file.fileType is KotlinFileType) {
        PsiManager.getInstance(project).findFile(file)
    } else null
}