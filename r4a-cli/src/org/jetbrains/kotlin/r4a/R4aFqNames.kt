package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.types.KotlinType

object R4aFqNames {
    val Composable = R4aUtils.r4aFqName("Composable")
    val Pivotal = R4aUtils.r4aFqName("Pivotal")
    val Children = R4aUtils.r4aFqName("Children")
    val Memoizable = R4aUtils.r4aFqName("Memoized")
    val Emittable = R4aUtils.r4aFqName("Emittable")
    val HiddenAttribute = R4aUtils.r4aFqName("HiddenAttribute")

    fun makeComposableAnnotation(module: ModuleDescriptor): AnnotationDescriptor = object : AnnotationDescriptor {
        override val type: KotlinType
            get() = module.findClassAcrossModuleDependencies(ClassId.topLevel(Composable))!!.defaultType
        override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
        override val source: SourceElement get() = SourceElement.NO_SOURCE
        override fun toString() = "[@Composable]"
    }
}

fun Annotated.hasComposableAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.Composable) != null
fun Annotated.hasPivotalAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.Pivotal) != null
fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.Children) != null
fun Annotated.hasMemoizableAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.Memoizable) != null
fun Annotated.hasEmittableAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.Emittable) != null
fun Annotated.hasHiddenAttributeAnnotation(): Boolean = annotations.findAnnotation(R4aFqNames.HiddenAttribute) != null