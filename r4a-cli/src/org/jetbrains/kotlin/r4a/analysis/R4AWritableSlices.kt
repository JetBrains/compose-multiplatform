/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

object R4AWritableSlices {
    val KTX_TAG_TYPE_DESCRIPTOR: WritableSlice<KtExpression, DeclarationDescriptor> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_TAG_COMPONENT_TYPE: WritableSlice<KtxElement, Int> = BasicWritableSlice(RewritePolicy.DO_NOTHING)

    val KTX_ATTR_DESCRIPTOR: WritableSlice<KtxAttribute, DeclarationDescriptor> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    val KTX_ATTR_TYPE: WritableSlice<KtxAttribute, KotlinType> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
