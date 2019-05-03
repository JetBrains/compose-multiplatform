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

package androidx.compose.plugins.idea.quickfix

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
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConvertCallToKtxQuickFix(
    private val expression: KtCallExpression
) : KotlinQuickFixAction<KtElement>(expression) {
    companion object MyFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val target = ComposeErrors.SVC_INVOCATION.cast(
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