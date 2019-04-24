/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConvertCallToKtxQuickFix(
    private val expression: KtCallExpression
) : KotlinQuickFixAction<KtElement>(expression) {
    companion object MyFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val target = R4AErrors.SVC_INVOCATION.cast(
                Errors.PLUGIN_ERROR.cast(diagnostic).a.diagnostic
            ).psiElement
            val foundPsiElement = target.containingCallExpression() ?: return null

            return ConvertCallToKtxQuickFix(foundPsiElement)
        }
    }

    override fun getFamilyName() = "KTX"

    override fun getText() = "Convert to a tag"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val bindingContext = expression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        val call = bindingContext.get(BindingContext.CALL, expression.calleeExpression)
        val resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, call) ?: return

        val factory = KtPsiFactory(expression)

        // Create a KTX_ELEMENT node with dummy values for the arguments
        val ktxElement = factory.createBlock(
            "<Test ${
            resolvedCall.valueArguments.map { "${it.key.name}=1" }.joinToString(" ")
            } />"
        ).statements.first() as? KtxElement ?: return

        // Replace the dummy arguments with the existing expressions
        var index = 0
        resolvedCall.valueArguments.forEach { _, resolvedValueArgument ->
            val ktxAttribute = ktxElement.attributes[index++]
            val originalReference = resolvedValueArgument.arguments.first() as? PsiElement
            val originalValue = originalReference?.getChildOfType<KtExpression>() as? PsiElement
            originalValue?.let { ktxAttribute.value?.replace(it) }
        }

        expression.replace(ktxElement)
    }
}

fun KtElement.containingCallExpression(): KtCallExpression? =
    this as? KtCallExpression ?: (this.parent as? KtElement)?.containingCallExpression()