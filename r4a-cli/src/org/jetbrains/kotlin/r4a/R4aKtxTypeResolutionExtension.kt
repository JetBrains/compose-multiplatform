package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade


class R4aKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        val ktxCallResolver = KtxCallResolver(callResolver, facade, element.project)

        val success = ktxCallResolver.resolveComposer(element, context)

        if (!success) {
            return fallback(element, context, facade)
        }

        val temporaryForKtxCall = TemporaryTraceAndCache.create(context, "trace to resolve ktx call", element)

        val resolvedKtxElementCall = ktxCallResolver.resolve(
            element,
            context.replaceTraceAndCache(temporaryForKtxCall)
        )

        temporaryForKtxCall.commit()

        context.trace.record(R4AWritableSlices.RESOLVED_KTX_CALL, element, resolvedKtxElementCall)
    }

    private fun fallback(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade
    ) {
        // If we can't resolve the tag name, we can't accurately report errors on the element, but we still want the type system to traverse
        // through all of the children nodes so that specific errors on those can get recorded
        with(element) {
            attributes.forEach { facade.checkType(it, context) }
            body?.forEach { facade.checkType(it, context) }
        }
    }

}