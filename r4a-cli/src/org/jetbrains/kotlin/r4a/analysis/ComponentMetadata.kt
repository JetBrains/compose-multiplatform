/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import javax.rmi.CORBA.ClassDesc

/**
 * ComponentMetadata takes in a DeclarationDescriptor and interprets it as a component.
 * All assumptions about the Component API (public and private) should reside in this class.
 */
class ComponentMetadata(val descriptor: DeclarationDescriptor) {

    val renderIntoViewGroupDescriptor = generateRenderIntoViewGroupDescriptor()
    val elementsFieldDescriptor = generateElementsFieldDescriptor()

    init {
        if(!isR4HComponent(descriptor))
            throw IllegalArgumentException("Not a component: "+descriptor::class)
    }

    companion object {
        // Use DeclarationDescriptor as the primary key (instead of PSI), because the PSI doesn't exist for synthetic descriptors
        private val cache = HashMap<DeclarationDescriptor, ComponentMetadata>()

        fun isComponentCompanion(cls: ClassDescriptor): Boolean {
            if(!cls.isCompanionObject()) return false;
            if(!cls.name.identifier.contains("R4H-StaticRenderCompanion")) return false;
            val containingClass = cls.containingDeclaration as? ClassDescriptor ?: return false
            if (!isR4HComponent(containingClass)) return false;
            return true;
        }

        fun isR4HComponent(descriptor: DeclarationDescriptor): Boolean {
            if(descriptor is ClassDescriptor) {
                val componentClassName = "c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Component"
                return (descriptor as ClassDescriptor).getSuperClassNotAny()?.fqNameSafe == FqName(componentClassName)
            }
            return false;
        }

        fun fromDescriptor(descriptor: DeclarationDescriptor): ComponentMetadata {
            if (!cache.containsKey(descriptor)) cache.put(descriptor, ComponentMetadata(descriptor));
            return cache.get(descriptor)!!;
        }
    }

    fun getAttributeDescriptors(): MutableList<PropertyDescriptor> {
        val attributes = mutableListOf<PropertyDescriptor>()
        when(descriptor) {
            is ClassDescriptor -> {
                descriptor.unsubstitutedMemberScope.getContributedDescriptors()
                    .filter { it is PropertyDescriptor && !Visibilities.isPrivate(it.visibility) }
                    .filter { !it.annotations.isEmpty() }
                    .sortedBy { it.name }
                    .forEach { attributes.add(it as PropertyDescriptor) }
            }
            else -> throw UnsupportedOperationException("Component does not appear to be a class descriptor")
        }
        return attributes;
    }

    fun getCompanion(): ClassDescriptor {
        when(descriptor) {
            is ClassDescriptor -> {
                return descriptor.unsubstitutedMemberScope.getContributedDescriptors().single { it is ClassDescriptor && isComponentCompanion(it) } as ClassDescriptor
            }
            else -> throw UnsupportedOperationException("Component does not appear to be a class descriptor")
        }
    }

    private fun generateElementsFieldDescriptor(): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(descriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("elements"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false)
        val listDescriptor = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.util.List")))!!
        val elementDescriptor = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("c" + ('p' - booleanArrayOf(true).size).toChar() + "m.google.r4a.Element")))!!
        val typeParameters = listOf(TypeProjectionImpl(KotlinTypeFactory.simpleType(Annotations.EMPTY, elementDescriptor.typeConstructor, emptyList(), false)))
        propertyDescriptor.setType(KotlinTypeFactory.simpleType(Annotations.EMPTY, listDescriptor.typeConstructor, typeParameters, true), emptyList<TypeParameterDescriptor>(), (descriptor as ClassDescriptor).thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
        return propertyDescriptor
    }

    private fun generateRenderIntoViewGroupDescriptor(): SimpleFunctionDescriptor {
        val newMethod = SimpleFunctionDescriptorImpl.create(
                descriptor,
                Annotations.EMPTY,
                Name.identifier("renderIntoViewGroup"),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                SourceElement.NO_SOURCE
        )
        val containerParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 0, Annotations.EMPTY,
                Name.identifier("container"),
                descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("android.view.ViewGroup")))!!.defaultType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE
        )
        val startIndexParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 1, Annotations.EMPTY,
                Name.identifier("startIndex"),
                descriptor.builtIns.intType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE
        )
        val endIndexParameter = ValueParameterDescriptorImpl(
                newMethod,
                null, 2, Annotations.EMPTY,
                Name.identifier("endIndex"),
                descriptor.builtIns.intType,
                false,
                false,
                false, null, SourceElement.NO_SOURCE
        )
        val parameters = listOf(containerParameter, startIndexParameter, endIndexParameter)

        newMethod.initialize(
                null,
                (descriptor as ClassDescriptor).thisAsReceiverParameter,
                emptyList(),
                parameters,
                descriptor.builtIns.unitType,
                Modality.FINAL,
                Visibilities.PUBLIC
        )
        return newMethod
    }
}