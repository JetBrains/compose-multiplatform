package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object R4AWritableSlices {
    val KTX_TAG_TYPE_DESCRIPTOR: WritableSlice<KtExpression, DeclarationDescriptor> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_TAG_INSTANCE_TYPE: WritableSlice<KtExpression, KotlinType> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_TAG_COMPOSABLE_TYPE: WritableSlice<KtxElement, ComposableType> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_TAG_CHILDRENLAMBDA: WritableSlice<KtxElement, R4aUtils.AttributeInfo> = BasicWritableSlice(RewritePolicy.DO_NOTHING)

    val KTX_ATTR_DESCRIPTOR: WritableSlice<KtxAttribute, DeclarationDescriptor> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_ATTR_TYPE: WritableSlice<KtxAttribute, KotlinType> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
