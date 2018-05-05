package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.r4a.GeneratedRerenderHelperClassDescriptor
import org.jetbrains.kotlin.r4a.GeneratedViewClassDescriptor
import org.jetbrains.kotlin.r4a.R4aUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl

/**
 * ComponentMetadata takes in a DeclarationDescriptor and interprets it as a component.
 * All assumptions about the Component API (public and private) should reside in this class.
 *
 * A ComponentMetadata can be used to get R4A synthetic method descirptors on a component,
 * as well as descriptors for the various synthetic helper classes.
 */
class ComponentMetadata(val descriptor: DeclarationDescriptor) {

    val wrapperViewDescriptor by lazy {
        GeneratedViewClassDescriptor(descriptor as ClassDescriptor)
    }

    val renderHelperClassDescriptor by lazy {
        GeneratedRerenderHelperClassDescriptor(this)
    }

    val renderIntoViewGroupDescriptor by lazy {
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
        newMethod
    }

    val elementsFieldDescriptor by lazy {
        val propertyDescriptor = PropertyDescriptorImpl.create(descriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE, true, Name.identifier("elements"), CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE, false, false, true, true, false, false)
        val listDescriptor = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName("java.util.List")))!!
        val elementDescriptor = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".Element")))!!
        val typeParameters = listOf(TypeProjectionImpl(KotlinTypeFactory.simpleType(Annotations.EMPTY, elementDescriptor.typeConstructor, emptyList(), false)))
        propertyDescriptor.setType(KotlinTypeFactory.simpleType(Annotations.EMPTY, listDescriptor.typeConstructor, typeParameters, true), emptyList<TypeParameterDescriptor>(), (descriptor as ClassDescriptor).thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
        propertyDescriptor
    }

    init {
        if(!isR4AComponent(descriptor))
            throw IllegalArgumentException("Not a component: "+descriptor::class)
    }

    companion object {
        // Use DeclarationDescriptor as the primary key (instead of PSI), because the PSI doesn't exist for synthetic descriptors
        private val cache = HashMap<DeclarationDescriptor, ComponentMetadata>()

        fun isComponentCompanion(cls: ClassDescriptor): Boolean {
            if(!cls.isCompanionObject()) return false;
            if(!cls.name.identifier.contains("R4H-StaticRenderCompanion")) return false;
            val containingClass = cls.containingDeclaration as? ClassDescriptor ?: return false
            if (!isR4AComponent(containingClass)) return false;
            return true;
        }

        fun isR4AComponent(descriptor: DeclarationDescriptor): Boolean {
            if(descriptor !is ClassDescriptor) return false
            val baseComponentDescriptor = descriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(R4aUtils.generateR4APackageName()+".Component"))) ?: return false
            return descriptor.isSubclassOf(baseComponentDescriptor)
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
}