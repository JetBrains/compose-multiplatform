/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.tools.compose

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingVisitorExtension
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

// Used to apply styles for calls to @Composable functions.
class ComposableHighlighter : KotlinHighlightingVisitorExtension() {
    override fun highlightDeclaration(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor): TextAttributesKey? {
        return null
    }

    override fun highlightCall(elementToHighlight: PsiElement, resolvedCall: ResolvedCall<*>): TextAttributesKey? {
        return if (resolvedCall.isComposableInvocation()) COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY else null
    }

    companion object TextAttributeRegistry {
        val COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY: TextAttributesKey
        const val COMPOSABLE_CALL_TEXT_ATTRIBUTES_NAME = "ComposableCallTextAttributes"

        init {
            COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY = TextAttributesKey.createTextAttributesKey(
                COMPOSABLE_CALL_TEXT_ATTRIBUTES_NAME,
                DefaultLanguageHighlighterColors.FUNCTION_CALL)
        }
    }
}

