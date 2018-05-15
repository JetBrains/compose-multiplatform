package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.r4a.analysis.ComponentMetadata
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.types.SimpleType

class WrapperViewSettersGettersResolveExtension : SyntheticResolveExtension {

    override fun generateSyntheticMethods(thisDescriptor: ClassDescriptor, name: Name, ctx: LazyClassContext, fromSupertypes: List<SimpleFunctionDescriptor>, result: MutableCollection<SimpleFunctionDescriptor>) {

        if (!ComponentMetadata.isWrapperView(thisDescriptor)) return

        result.addAll((thisDescriptor as GeneratedViewClassDescriptor).attributeSetters)
    }

}
