/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import com.sun.jndi.ldap.LdapPoolManager.trace
import org.jetbrains.kotlin.contracts.model.structure.UNKNOWN_COMPUTATION.type
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

/**
 * If a lambda is marked as `@Composable`, then the inferred type should become `@Composable`
 */
class R4aTypeResolutionInterceptorExtension : TypeResolutionInterceptorExtension {

    override fun interceptType(element: KtElement, context: ExpressionTypingContext, type: KotlinType): KotlinType {
        if(type == null || type === TypeUtils.NO_EXPECTED_TYPE) return type
        if(element !is KtLambdaExpression) return type
        val module = context.scope.ownerDescriptor.module
        val checker = StorageComponentContainerContributor.getInstances(element.project).single { it is ComposableAnnotationChecker } as ComposableAnnotationChecker
        if(element is KtLambdaExpression && (checker.hasComposableAnnotation(context.expectedType) || checker.analyze(context.trace, element, type))) {
            val annotation = object : AnnotationDescriptor {
                override val type: KotlinType get() = module.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("Composable")))!!.defaultType
                override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
                override val source: SourceElement get() = SourceElement.NO_SOURCE
                override fun toString() = "[@Composable]"
            }
            return type.replaceAnnotations(AnnotationsImpl(type.annotations + annotation))
        }
        return type
    }
}
