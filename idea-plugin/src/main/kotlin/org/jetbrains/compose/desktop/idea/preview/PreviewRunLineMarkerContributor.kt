/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.idea.preview

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

class PreviewRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        // Marker should be in a single LeafPsiElement. We choose the identifier and return null for other elements within the function.
        if (element !is LeafPsiElement) return null
        if (element.node.elementType != KtTokens.IDENTIFIER) return null

        val parent = element.parent
        return when {
            parent is KtNamedFunction && parent.isValidComposePreview() ->
                Info(
                    PreviewIcons.COMPOSE,
                    arrayOf(ExecutorAction.getActions(0).first())
                ) { PreviewMessages.runPreview(parent.name!!) }
            else -> null
        }
    }
}

