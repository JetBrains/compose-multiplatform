package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.ComposableAnnotationChecker
import org.jetbrains.kotlin.r4a.KtxAttributeInfo
import org.jetbrains.kotlin.r4a.KtxTagInfo
import org.jetbrains.kotlin.r4a.ast.ResolvedKtxElementCall
import org.jetbrains.kotlin.util.slicedMap.*

object R4AWritableSlices {
    val KTX_TAG_INFO: WritableSlice<KtxElement, KtxTagInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_ATTR_INFO: WritableSlice<KtxAttribute, KtxAttributeInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val COMPOSABLE_ANALYSIS: WritableSlice<KtElement, ComposableAnnotationChecker.Composability> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val RESOLVED_KTX_CALL: WritableSlice<KtxElement, ResolvedKtxElementCall> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val ATTRIBUTE_KEY_REFERENCE_TARGET: WritableSlice<KtReferenceExpression, Set<DeclarationDescriptor>> = BasicWritableSlice(REWRITES_ALLOWED)
}

private val REWRITES_ALLOWED = object : RewritePolicy {
    override fun <K : Any?> rewriteProcessingNeeded(key: K): Boolean = true
    override fun <K : Any?, V : Any?> processRewrite(slice: WritableSlice<K, V>?, key: K, oldValue: V, newValue: V): Boolean = true
}
