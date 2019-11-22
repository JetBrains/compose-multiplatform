/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.plugins.idea

import androidx.compose.plugins.kotlin.ComposableEmitDescriptor
import androidx.compose.plugins.kotlin.ComposableFunctionDescriptor
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

// Used to apply styles for calls to @Composable functions.
class IdeComposableAnnotator : Annotator {
    companion object TextAttributeRegistry {
        private val COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY: TextAttributesKey
        private val ANALYSIS_RESULT_KEY = Key<AnalysisResult>(
            "IdeComposableAnnotator.DidAnnotateKey"
        )

        init {
            COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY = TextAttributesKey.createTextAttributesKey(
                "ComposableCallTextAttributes", DefaultLanguageHighlighterColors.FUNCTION_CALL)
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is KtCallExpression) return

        if (!isComposeEnabled(element)) return

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
        val annotation: Annotation = holder.createInfoAnnotation(elementToStyle.textRange, null)
        annotation.textAttributes = COMPOSABLE_CALL_TEXT_ATTRIBUTES_KEY
    }

    private fun shouldStyleCall(bindingContext: BindingContext, element: KtCallExpression):
            Boolean {
        return when (element.getResolvedCall(bindingContext)?.candidateDescriptor) {
            is ComposableEmitDescriptor -> true
            is ComposableFunctionDescriptor -> true
            else -> false
        }
    }
}