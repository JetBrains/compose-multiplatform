/*
 * Copyright 2018 The Android Open Source Project
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

package org.jetbrains.kotlin.r4a.idea.quickfix

import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.r4a.R4aFqNames

class AnnotateWithComposableQuickFix(fn: KtElement) : AbstractKtxQuickFix<KtElement>(fn) {
    companion object MyFactory : AbstractFactory(
        {
            val element = psiElement
            if (element.node.elementType == KtTokens.IDENTIFIER) {
                // if it is an identifier, it is because it is a named function declaration that
                // needs to be annotated as @Composable. Here we find the function itself, and pass
                // it to the quickfix
                val targetFn: KtElement? = findElement<KtFunction>()
                targetFn?.let { AnnotateWithComposableQuickFix(it) }
            } else when (element) {
                // if it is a lambda expression, then we should annotate the expression itself
                is KtLambdaExpression -> AnnotateWithComposableQuickFix(element)
                else -> null
            }
        }
    )

    override fun getText() = "Annotate with ''@Composable''"

    override fun invoke(ktPsiFactory: KtPsiFactory, element: KtElement) {
        when (element) {
            // add it to the list of modifiers
            is KtModifierListOwner -> element.addAnnotation(R4aFqNames.Composable)
            // annotate the expression itself
            is KtLambdaExpression -> element.addAnnotation(R4aFqNames.Composable)
            else -> error("Expected a KtModifierListOwner or KtLambdaExpression. Found ${
                element.node.elementType
            }")
        }
    }
}

private fun KtLambdaExpression.addAnnotation(
    annotationFqName: FqName,
    annotationInnerText: String? = null
): Boolean {
    val annotationText = when (annotationInnerText) {
        null -> "@${annotationFqName.asString()}"
        else -> "@${annotationFqName.asString()}($annotationInnerText)"
    }

    val psiFactory = KtPsiFactory(this)
    val annotatedExpression = psiFactory.createExpression("$annotationText $text")
    val parent = parent as? KtElement
    replace(annotatedExpression)
    parent?.let { ShortenReferences.DEFAULT.process(it) }

    return true
}