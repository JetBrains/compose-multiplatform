/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.analysis.R4AErrors.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isUnit

class R4aKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade
    ) {
        val openingTagExpr = element.qualifiedTagName ?: element.simpleTagName ?: return

        val resolutionFacade = element.getResolutionFacade()

        val openingDescriptor = R4aUtils.resolveDeclaration(
            expression = openingTagExpr,
            moduleDescriptor = context.scope.ownerDescriptor.module,
            trace = context.trace,
            scopeForFirstPart = context.scope
        ) ?: return

        val possibleAttributes = R4aUtils.getPossibleAttributesForDescriptor(openingDescriptor, context.scope, resolutionFacade)

        validateTagDescriptor(element, openingTagExpr, possibleAttributes, openingDescriptor, context, facade)

        context.trace.record(BindingContext.KTX_TAG_TYPE_DESCRIPTOR, element, openingDescriptor)

        element.attributes?.let { attributes ->
            for (attribute in attributes) {
                visitKtxAttributeAfterElement(attribute, possibleAttributes, element, context, facade)
            }
        }

        // Do this at the end so we can return if no closing tag
        val closingTagExpr = element.qualifiedClosingTagName ?: element.simpleClosingTagName ?: return

        // resolving this allows for Cmd-Click to work on the closing tag as well as the opening tag, which we want.
        val closingDescriptor = R4aUtils.resolveDeclaration(
            expression = closingTagExpr,
            moduleDescriptor = context.scope.ownerDescriptor.module,
            trace = context.trace,
            scopeForFirstPart = context.scope
        ) ?: return
    }

    private fun visitKtxAttributeAfterElement(
        attribute: KtxAttribute,
        possibleAttributes: Collection<R4aUtils.AttributeInfo>,
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade
    ) {
        val descriptor = context.trace[BindingContext.KTX_TAG_TYPE_DESCRIPTOR, element] ?: return

        val valueExpr = attribute.value ?: return // TODO: allow for attribute "punning"
        val keyNode = attribute.key ?: return
        val keyStr = keyNode.text

        val namedAttributes = possibleAttributes
            .filter { attr -> attr.name == keyStr }

        val resolvedAttribute = namedAttributes
            .firstOrNull { param ->
                val temporaryTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve ktx attribute", valueExpr)
                val newContext = context
                    .replaceExpectedType(param.type)
                    .replaceBindingTrace(temporaryTrace)
                    .replaceContextDependency(ContextDependency.INDEPENDENT)

                val valueType = facade.getTypeInfo(valueExpr, newContext).type ?: return@firstOrNull false
                if (valueType.isSubtypeOf(param.type)) {
                    // its valid
                    context.trace.record(BindingContext.REFERENCE_TARGET, keyNode, param.descriptor)
                    newContext.trace.record(BindingContext.REFERENCE_TARGET, keyNode, param.descriptor)
                    temporaryTrace.commit()
                    true
                } else false
            }

        if (resolvedAttribute != null) return
        val param = possibleAttributes.find { attr -> attr.name == keyStr }
        if (param != null) {
            val newContext = context.replaceExpectedType(param.type)
            val valueType = facade.getTypeInfo(valueExpr, newContext).type ?: return

            context.trace.reportFromPlugin(
                MISMATCHED_ATTRIBUTE_TYPE.on(attribute, keyStr, param.type, valueType),
                R4ADefaultErrorMessages
            )
        } else {
            val valueType = facade.getTypeInfo(valueExpr, context).type ?: return
            context.trace.reportFromPlugin(
                UNRESOLVED_ATTRIBUTE_KEY.on(attribute, descriptor, keyStr, valueType),
                R4ADefaultErrorMessages
            )
        }
    }

    private fun validateTagDescriptor(
        element: KtxElement,
        tagExpression: KtExpression,
        possibleAttributes: Collection<R4aUtils.AttributeInfo>,
        descriptor: DeclarationDescriptor,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade
    ) {
        val module = context.scope.ownerDescriptor.module
        val attributes = element.attributes

        // NOTE: we may want to cache this
        val r4aComponentId = ClassId.topLevel(FqName("com.google.r4a.Component"))
        val r4aComponentDescriptor = module.findClassAcrossModuleDependencies(r4aComponentId) ?: return

        // NOTE: we may want to cache this
        val androidViewId = ClassId.topLevel(FqName("android.view.View"))
        val androidViewDescriptor = module.findClassAcrossModuleDependencies(androidViewId) ?: return

        // TODO(lmr): check visibility of opening tag descriptor

        // TODO(lmr): check to see if there is a special children type? see if body matches it?

        // TODO(lmr): if its a view (and not a component) and children are provided, ensure that its a ViewGroup subclass

        val requiredAttributes = possibleAttributes.filter { it.required }

        // report duplicate attributes on the element. this is never allowed
        if (attributes != null) {
            val duplicates = getDuplicates(attributes, { attr -> attr.key?.text ?: "" })

            for (dup in duplicates) {
                dup.key?.let {
                    context.trace.reportFromPlugin(
                        R4AErrors.DUPLICATE_ATTRIBUTE.on(it),
                        R4ADefaultErrorMessages
                    )
                }
            }

            val missing = getMissing(
                requiredAttributes,
                attributes,
                { param -> param.name },
                { attr -> attr.key?.text ?: "" }
            )
            if (missing.isNotEmpty()) {
                context.trace.reportFromPlugin(
                    MISSING_REQUIRED_ATTRIBUTES.on(element, missing.map { it.descriptor }),
                    R4ADefaultErrorMessages
                )
            }
        } else {
            if (requiredAttributes.isNotEmpty()) {
                // TODO(lmr): we will probably need to add special treatment for children types...
                context.trace.reportFromPlugin(
                    MISSING_REQUIRED_ATTRIBUTES.on(element, requiredAttributes.map { it.descriptor }),
                    R4ADefaultErrorMessages
                )
            }
        }

        when (descriptor) {
            is ClassDescriptor -> {
                if (!descriptor.isSubclassOf(r4aComponentDescriptor) && !descriptor.isSubclassOf(androidViewDescriptor)) {
                    val validTypes = listOf(r4aComponentDescriptor.defaultType, androidViewDescriptor.defaultType)
                    context.trace.reportFromPlugin(
                        INVALID_TAG_TYPE.on(tagExpression, descriptor.defaultType, validTypes),
                        R4ADefaultErrorMessages
                    )
                }
                // TODO(lmr): mark whether or not its a component or a view, so we can highlight after-the-fact differently for each
            }
            is FunctionDescriptor -> {
                if (descriptor.isSuspend) {
                    // we don't allow suspend functions to be renderable (yet?)
                    context.trace.reportFromPlugin(SUSPEND_FUNCTION_USED_AS_SFC.on(tagExpression), R4ADefaultErrorMessages)
                }
                // TODO(lmr): do we want to ensure that the function actually has ktx in it?
                descriptor.returnType?.let {
                    if (!it.isUnit()) {
                        context.trace.reportFromPlugin(INVALID_TYPE_SIGNATURE_SFC.on(tagExpression), R4ADefaultErrorMessages)
                    }
                }
            }
            else -> {
                val validTypes = listOf(r4aComponentDescriptor.defaultType, androidViewDescriptor.defaultType)
                context.trace.reportFromPlugin(INVALID_TAG_DESCRIPTOR.on(tagExpression, validTypes), R4ADefaultErrorMessages)
            }
        }
    }

    private fun <T, V> getMissing(required: List<T>, provided: List<V>, keyRequired: (T) -> String, keyProvided: (V) -> String): List<T> {
        val requiredMap = HashMap<String, T>()
        for (el in required) requiredMap[keyRequired(el)] = el

        for (el in provided) {
            val key = keyProvided(el)
            val value = requiredMap[key]
            if (value != null) {
                requiredMap.remove(key)
            }
        }

        return requiredMap.values.toList()
    }

    private fun <T> getDuplicates(elements: List<T>, keyFn: (T) -> String): List<T> {
        val result = mutableListOf<T>()
        val set = HashSet<String>()
        for (el in elements) {
            val key = keyFn(el)
            if (set.contains(key)) {
                result.add(el)
            } else {
                set.add(key)
            }
        }

        return result
    }

}