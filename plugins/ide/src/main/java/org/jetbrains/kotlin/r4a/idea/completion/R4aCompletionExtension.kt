package org.jetbrains.kotlin.r4a.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import org.jetbrains.kotlin.idea.completion.CompletionSessionConfiguration
import org.jetbrains.kotlin.idea.completion.KotlinCompletionExtension
import org.jetbrains.kotlin.idea.completion.ToFromOriginalFileMapper
import org.jetbrains.kotlin.idea.editor.fixers.range
import org.jetbrains.kotlin.lexer.KtTokens.EQ
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.idea.parentOfType

class R4aCompletionExtension : KotlinCompletionExtension() {
    override fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val expr = parameters.position
        val superParent = expr.parent?.parent

        when (superParent) {
            is KtDotQualifiedExpression -> {
                val ktxEl = superParent.parentOfType<KtxElement>()
                if (ktxEl != null && ktxEl.qualifiedTagName?.range?.contains(expr.range) == true) {
                    return performKtxTagCompletion(parameters, result)
                }
            }
            is KtxElement -> {
                return performKtxTagCompletion(parameters, result)
            }
            is KtxAttribute -> {
                if (expr.parent?.prevSibling?.node?.elementType == EQ) {
                    // we are inside of a ktx expression value... use normal autocomplete
                    return false
                }
                return performKtxAttributeCompletion(parameters, result)
            }
        }

        return false
    }

    private fun performKtxAttributeCompletion(
        params: CompletionParameters,
        result: CompletionResultSet
    ): Boolean {
        val session = R4aAttributeCompletionSession(
            CompletionSessionConfiguration(params),
            params,
            ToFromOriginalFileMapper.create(params),
            result
        )

        if (!session.isValid()) return false

        session.complete()

        return true
    }

    private fun performKtxTagCompletion(
        params: CompletionParameters,
        result: CompletionResultSet
    ): Boolean {
        R4aTagCompletionSession(
            CompletionSessionConfiguration(params),
            params,
            ToFromOriginalFileMapper.create(params),
            result
        ).complete()

        return true
    }
}
