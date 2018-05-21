package org.jetbrains.kotlin.r4a.frames.analysis

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType

/**
 * Helpers for creating the new properties, fields and properties of the framed class and the framed record
 *
 * - Framed properties are all public properties of the class.
 * - A framed record has
 *   - a corresponding property for each of the framed class' public properties
 *   - a `create` method that creates a new instance of the record
 *   - an `assign` method that copies values from the value into the record
 * - All framed properties redirect to the current readable or writable record for the class corresponding to
 *   the current open frame.
 *
 *   For example, given the declaration:
 *
 *   class MyComponent: Component {
 *     var some: String = "Default some"
 *     var data: String = "Default data"
 *
 *     override compose() { ... }
 *   }
 *
 *   The class is transformed into something like:
 *
 *   class MyComponent: Component {
 *     var some: String
 *       get() = (_readable(next) as MyComponent_Record).some
 *       set(value) { (_writable(next) as MyComponent_Record).some = value }
 *     var data: String
 *       get() = ((_readable(next) as MyComponent_Record).data
 *       set(value) { (_writable(next, this) as MyComponent_Record).data = value }
 *
 *     init {
 *       next = MyComponent_Record()
 *       (next as MyComponent_Record).some = "Default some"
 *       (next as MyComponent_Record).data = "Default data"
 *     }
 *   }
 *
 *   class MyComponent_Record : AbstractRecord {
 *     @JvmField var some: String
 *     @JvmField var data: String
 *
 *     override fun create(): Record = MyComponent_Record()
 *     override fun assign(value: Record) {
 *       some = (value as MyComponent_Record).some
 *       data = (value as MyCompoennt_Record).data
 *     }
 *   }
 */
class FrameMetadata(private val framedClassDescriptor: ClassDescriptor) {
    /**
     * Get the list of properties on the framed class that should be framed
     */
    fun getFramedProperties() = myFramedProperties

    /**
     * Get the list of the record's properties (on for each public property of the framed object)
     */
    fun getRecordPropertyDescriptors(recordClassDescriptor: ClassDescriptor): List<PropertyDescriptor> {
        return myFramedProperties.map { syntheticProperty(recordClassDescriptor, it.name, it.returnType!!) }
    }

    /**
     * Get the list of the record's methods (create and assign)
     */
    fun getRecordMethodDescriptors(container: ClassDescriptor, recordDescriptor: ClassDescriptor): List<SimpleFunctionDescriptor> {
        return listOf(
            // fun create(): <record>
            syntheticMethod(container, "create", recordDescriptor.defaultType),
            // fun assign(value: <record>)
            syntheticMethod(container, "assign", container.builtIns.unitType,
                Parameter("value", recordDescriptor.defaultType)))
    }

    private val myFramedProperties: List<PropertyDescriptor> by lazy {
        framedClassDescriptor.unsubstitutedMemberScope.getContributedDescriptors().mapNotNull {
            if (it is PropertyDescriptor &&
                it.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                it.isVar &&
                (!Visibilities.isPrivate(it.visibility))
            ) it else null
        }
    }
}

private fun syntheticProperty(
    container: ClassDescriptor, name: Name,
    type: KotlinType,
    visibility: Visibility = Visibilities.PUBLIC,
    readonly: Boolean = false
): PropertyDescriptor =
    PropertyDescriptorImpl.create(
        container, Annotations.EMPTY, Modality.OPEN, Visibilities.PUBLIC, true,
        name, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
        false, false, true, true, false,
        false).apply {
        val getter = PropertyGetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.OPEN,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            SourceElement.NO_SOURCE
        ).apply { initialize(type) }
        val setter = if (readonly) null else PropertySetterDescriptorImpl(
            this,
            Annotations.EMPTY,
            Modality.OPEN,
            visibility,
            false,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            SourceElement.NO_SOURCE
        ).apply {
            initialize(PropertySetterDescriptorImpl.createSetterParameter(this, type))
        }
        initialize(getter, setter)
        setType(type, emptyList<TypeParameterDescriptor>(), container.thisAsReceiverParameter, null as ReceiverParameterDescriptor?)
    }

private data class Parameter(val name: Name, val type: KotlinType) {
    constructor(name: String, type: KotlinType) : this(Name.identifier(name), type)
}

private fun syntheticMethod(
    container: ClassDescriptor,
    name: Name,
    returnType: KotlinType,
    vararg parameters: Parameter
): SimpleFunctionDescriptor =
    SimpleFunctionDescriptorImpl.create(
        container,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        SourceElement.NO_SOURCE
    ).apply {
        val parameterDescriptors = parameters.map {
            ValueParameterDescriptorImpl(
                this,
                null,
                0,
                Annotations.EMPTY,
                it.name,
                it.type,
                false,
                false,
                false,
                null,
                SourceElement.NO_SOURCE
            )
        }

        initialize(
            null,
            container.thisAsReceiverParameter,
            emptyList<TypeParameterDescriptor>(),
            parameterDescriptors,
            returnType,
            Modality.FINAL,
            Visibilities.PUBLIC
        )
    }

private fun syntheticMethod(
    container: ClassDescriptor,
    name: String,
    returnType: KotlinType,
    vararg parameters: Parameter
): SimpleFunctionDescriptor =
    syntheticMethod(container, Name.identifier(name), returnType, *parameters)
