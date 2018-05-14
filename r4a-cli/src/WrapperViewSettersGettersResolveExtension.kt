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
        val wrapperView = thisDescriptor as GeneratedViewClassDescriptor

        val metadata = ComponentMetadata.fromDescriptor(wrapperView.containingDeclaration)

        val unitType : SimpleType = thisDescriptor.builtIns.unitType

        for (attr in metadata.getAttributeDescriptors()) {
            val newMethod = SimpleFunctionDescriptorImpl.create(
                metadata.wrapperViewDescriptor,
                Annotations.EMPTY,
                Name.identifier(R4aUtils.setterMethodFromPropertyName(attr.name.identifier)),
                CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE
            )

            newMethod.initialize(
                null,
                null,
                emptyList(),
                listOf(
                    ValueParameterDescriptorImpl(
                        newMethod,
                        null,
                        0,
                        Annotations.EMPTY,
                        attr.name,
                        attr.type,
                        false,
                        false,
                        false,
                        null,
                        SourceElement.NO_SOURCE
                    )
                ),
                unitType,
                Modality.FINAL,
                Visibilities.PUBLIC
            )

            result.add(newMethod)
        }
    }

}
