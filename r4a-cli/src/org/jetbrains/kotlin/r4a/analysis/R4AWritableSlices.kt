package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.ComposableAnnotationChecker
import org.jetbrains.kotlin.r4a.KtxAttributeInfo
import org.jetbrains.kotlin.r4a.KtxTagInfo
import org.jetbrains.kotlin.util.slicedMap.*

object R4AWritableSlices {
    val KTX_TAG_INFO: WritableSlice<KtxElement, KtxTagInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_ATTR_INFO: WritableSlice<KtxAttribute, KtxAttributeInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val COMPOSABLE_ANALYSIS: WritableSlice<KtElement, ComposableAnnotationChecker.Composability> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
