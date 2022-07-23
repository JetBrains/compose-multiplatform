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

import com.android.tools.modules.*
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

// Used to apply styles for calls to @Composable functions.
class ComposableAnnotator : Annotator {
    companion object TextAttributeRegistry {
        val COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY: TextAttributesKey
        val COMPOSABLE_CALL_TEXT_ATTRIBUTES_NAME = "ComposableCallTextAttributes"
        private val ANALYSIS_RESULT_KEY = Key<AnalysisResult>(
            "ComposableAnnotator.DidAnnotateKey"
        )
        private val CAN_CONTAIN_COMPOSABLE_KEY = Key<Boolean>(
            "ComposableAnnotator.CanContainComposable"
        )

        init {
            COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY = TextAttributesKey.createTextAttributesKey(
              COMPOSABLE_CALL_TEXT_ATTRIBUTES_NAME,
              DefaultLanguageHighlighterColors.FUNCTION_CALL)
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtCallExpression) return

        // AnnotationHolder.currentAnnotationSession applies to a single file.
        var canContainComposable = holder.currentAnnotationSession.getUserData(CAN_CONTAIN_COMPOSABLE_KEY)
        if (canContainComposable == null) {
            // isComposeEnabled doesn't work for library sources, we check all kt library sources files. File check only once on opening.
            canContainComposable = element.inComposeModule() ||
                    (element.containingFile.virtualFile != null &&
                            ProjectFileIndex.getInstance(element.project)
                                .isInLibrarySource(element.containingFile.virtualFile))
            holder.currentAnnotationSession.putUserData(CAN_CONTAIN_COMPOSABLE_KEY, canContainComposable)
        }

        if (!canContainComposable) return

        // AnnotationHolder.currentAnnotationSession applies to a single file.
        var analysisResult = holder.currentAnnotationSession.getUserData(
            ANALYSIS_RESULT_KEY
        )
        if (analysisResult == null) {
            val ktFile = element.containingFile as? KtFile ?: return
            analysisResult = ktFile.analyzeWithAllCompilerChecks()
            holder.currentAnnotationSession.putUserData(
              ANALYSIS_RESULT_KEY, analysisResult
            )
        }
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }
        if (!shouldStyleCall(analysisResult.bindingContext, element)) return
        val elementToStyle = element.calleeExpression ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(elementToStyle).textAttributes(COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY).create()
    }

    private fun shouldStyleCall(bindingContext: BindingContext, element: KtCallExpression): Boolean {
        return element.getResolvedCall(bindingContext)?.isComposableInvocation() == true
    }
}