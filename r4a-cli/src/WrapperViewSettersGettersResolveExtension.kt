package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.types.SimpleType

class WrapperViewSettersGettersResolveExtension : SyntheticResolveExtension {

    override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
        return null
    }

    override fun generateSyntheticMethods(cls: ClassDescriptor, name: Name, ctx: LazyClassContext, fromSupertypes: List<SimpleFunctionDescriptor>, result: MutableCollection<SimpleFunctionDescriptor>) {

        if(name != Name.identifier("setMySweetAttribute")) return

        result.add(getSetterMethodDescriptor(cls))
    }

    fun getSetterMethodDescriptor(cls: ClassDescriptor) : SimpleFunctionDescriptor {

        val unitType : SimpleType = cls.builtIns.unitType
        val newMethod = SimpleFunctionDescriptorImpl.create(cls, Annotations.EMPTY, Name.identifier("setMySweetAttribute"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE)
        newMethod.initialize(null, null, emptyList(), emptyList(), unitType, Modality.FINAL, Visibilities.PUBLIC)

        return newMethod
    }

    private fun ClassConstructorDescriptor.isZeroParameterConstructor(): Boolean {
        val parameters = this.valueParameters
        return parameters.isEmpty()
               || (parameters.all { it.hasDefaultValue() } && (isPrimary || findJvmOverloadsAnnotation() != null))
    }
}
