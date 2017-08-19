/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.r4a.GeneratedViewClassDescriptor
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.HashSet

object R4AWritableSlices {
    public val COMPONENT_CLASSES: WritableSlice<ModuleDescriptor, HashSet<KtClass>> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
    public val WRAPPER_VIEW: WritableSlice<KtClass, GeneratedViewClassDescriptor> = BasicWritableSlice(RewritePolicy.DO_NOTHING)
}
