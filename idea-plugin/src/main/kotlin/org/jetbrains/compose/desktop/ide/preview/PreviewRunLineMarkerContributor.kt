/*
 * Copyright (C) 2019 The Android Open Source Project
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

package org.jetbrains.compose.desktop.ide.preview

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Based on
 * com.android.tools.idea.compose.preview.runconfiguration.ComposePreviewRunLineMarkerContributor from AOSP
 * with modifications
 */
class PreviewRunLineMarkerContributor : RunLineMarkerContributor() {
    override fun getInfo(element: PsiElement): Info? {
        // Marker should be in a single LeafPsiElement. We choose the identifier and return null for other elements within the function.
        if (element !is LeafPsiElement) return null
        if (element.node.elementType != KtTokens.IDENTIFIER) return null

        return when (val parent = element.parent) {
            is KtNamedFunction -> {
                val previewFunction = parent.asPreviewFunctionOrNull() ?: return null
                val actions = arrayOf(RunPreviewAction(previewFunction))
                Info(PreviewIcons.COMPOSE, actions) { PreviewMessages.runPreview(parent.name!!) }
            }
            else -> null
        }
    }
}
