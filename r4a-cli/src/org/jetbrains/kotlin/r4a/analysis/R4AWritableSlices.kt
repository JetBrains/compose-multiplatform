package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.ComposableAnnotationChecker
import org.jetbrains.kotlin.r4a.KtxAttributeInfo
import org.jetbrains.kotlin.r4a.KtxTagInfo
import org.jetbrains.kotlin.r4a.ast.ResolvedKtxElementCall
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object R4AWritableSlices {
    val KTX_TAG_INFO: WritableSlice<KtxElement, KtxTagInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_ATTR_INFO: WritableSlice<KtxAttribute, KtxAttributeInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val COMPOSABLE_ANALYSIS: WritableSlice<KtElement, ComposableAnnotationChecker.Composability> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val RESOLVED_KTX_CALL: WritableSlice<KtxElement, ResolvedKtxElementCall> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
