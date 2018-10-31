package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.analysis.R4AErrors.MISSING_REQUIRED_ATTRIBUTES
import org.jetbrains.kotlin.r4a.analysis.R4AErrors.UNRESOLVED_ATTRIBUTE_KEY
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.KTX_ATTR_INFO
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.KTX_TAG_INFO
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade


private fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(R4aUtils.r4aFqName("Children")) != null

class R4aKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        if (R4AFlags.USE_NEW_TYPE_RESOLUTION) visitKtxElementNew(element, context, facade, callResolver)
        else visitKtxElementOld(element, context, facade, callResolver)
    }

    fun visitKtxElementNew(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        val ktxCallResolver = KtxCallResolver(callResolver, facade, element.project)

        val foundComposer = ktxCallResolver.findComposer(element, context)

        if (!foundComposer) {
            // TODO(lmr): proper diagnostics for missing composer
            return fallback(element, context, facade)
        }

        val temporaryForKtxCall = TemporaryTraceAndCache.create(context, "trace to resolve ktx call", element)

        val resolvedKtxElementCall = ktxCallResolver.resolveTag(
            listOfNotNull(
                element.simpleTagName,
                element.simpleClosingTagName,
                element.qualifiedTagName,
                element.qualifiedClosingTagName
            ),
            element.attributes,
            element.bodyLambdaExpression,
            context.replaceTraceAndCache(temporaryForKtxCall)
        )

        temporaryForKtxCall.commit()

        if (resolvedKtxElementCall == null) {

        } else {
            context.trace.record(R4AWritableSlices.RESOLVED_KTX_CALL, element, resolvedKtxElementCall)
        }
    }

    fun visitKtxElementOld(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade,
        callResolver: CallResolver
    ) {
        // First we resolve the tag name to something we can "call". It can be one of the following:
        //
        //   1. A constructor (either component or view)
        //   2. A Function reference (function component)
        //   3. A simple or dot-qualified expression whose return type is callable (usually a children property)
        //
        // We resolve this by taking into account the current scope as well as the attributes passed into the element. The function's
        // parameters and type arguments will factor into the resolution.

        val moduleDescriptor = context.scope.ownerDescriptor.module

        val tagExpr = element.simpleTagName ?: element.qualifiedTagName ?: return fallback(element, context, facade)

        val attributeExpressions = element.attributes.map { it.key.getReferencedName() to (it.value ?: it.key!!) }.toMap()

        val tagResolver = KtxTagResolver(callResolver, facade, attributeExpressions, element.bodyLambdaExpression)

        val tag = tagResolver.resolveReference(tagExpr, context)

        if (tag == null) {
            // We couldn't find anything, so we mark things as unresolved
            element.simpleTagName?.let {
                // NOTE(lmr): we are explicitly using our own UNRESOLVED_TAG diagnostic here instead
                // of UNRESOLVED_REFERENCE because we want to do our own quick fix handling for tags,
                // and using UNRESOLVED_REFERENCE produces a conflict.
                context.trace.reportFromPlugin(
                    R4AErrors.UNRESOLVED_TAG.on(it, it),
                    R4ADefaultErrorMessages
                )
            }
            element.simpleClosingTagName?.let {
                // NOTE(lmr): we are explicitly using our own UNRESOLVED_TAG diagnostic here instead
                // of UNRESOLVED_REFERENCE because we want to do our own quick fix handling for tags,
                // and using UNRESOLVED_REFERENCE produces a conflict.
                context.trace.reportFromPlugin(
                    R4AErrors.UNRESOLVED_TAG.on(it, it),
                    R4ADefaultErrorMessages
                )
            }
            element.qualifiedTagName?.let {
                tagResolver.markUnresolved(it, context)
            }
            // if the tag is dot-qualified, we can't directly mark as unresolved, but the tagResolver handles each part of it, so we just
            // call it again on the closing tag, even though it's doing duplicate work from above and we know it won't resolve.
            element.qualifiedClosingTagName?.let {
                tagResolver.resolveReference(it, context)
                tagResolver.markUnresolved(it, context)
            }
            return fallback(element, context, facade)
        }

        tag.commit()

        // If we've made it here, it resolved to something and our opening tag is marked but our closing tag is not, so we copy it over
        element.simpleClosingTagName?.let {
            val ref = context.trace[BindingContext.REFERENCE_TARGET, element.simpleTagName]
            context.trace.record(BindingContext.REFERENCE_TARGET, it, ref)
        }

        // again, if it's dot-qualified we have to do the more expensive thing of calling resolveReference(...) again so it marks
        // the closing tag properly.
        element.qualifiedClosingTagName?.let {
            tagResolver.resolveReference(it, context)?.commit()
        }

        // NOTE(lmr): tag may not be valid at this point, but if it isn't a diagnostic will be reported on the tag. Regardless of whether
        // or not it's valid, we continue to validate the entire tag since we want to give the user as much information as possible. If one
        // tag constructor isn't valid, we still wan't to type check all of its children for instance.

        val ctorAttributes = when {
            tag.isConstructed -> tag.parameters.map { it.name }.toSet()
            else -> emptySet()
        }

        tagResolver.attributesInConstruction = ctorAttributes

        val attributeNameToDescriptor = tag.parameters
            .map { it.name to it.descriptor }
            .toMap()

        val attributeInfos = mutableListOf<KtxAttributeInfo>()

        // Note that some of the attributes in this list are already "consumed" by the constructor, but we iterate over them anyway to
        // perform additional checks
        for (attribute in element.attributes) {
            // TODO(lmr): check for a @HiddenAttribute annotation for properties/setters that we want to disallow for R4a

            val keyExpr = attribute.key!!
            val valueExpr = attribute.value ?: keyExpr
            val nameAsString = keyExpr.getReferencedName()

            var attributeInfo: KtxAttributeInfo? = null
            val expectedTypes = mutableListOf<KotlinType>()

            // we first look for "setFoo" methods to resolve the attribute to. This will include extension setter methods in scope.
            if (attributeInfo == null && tag.instanceType != null) {
                attributeInfo = tagResolver.resolveAttributeAsSetter(
                    nameAsString,
                    keyExpr,
                    valueExpr,
                    tag,
                    expectedTypes,
                    context
                )
            }

            // if no suitable setter method was found, we next look for properties
            if (attributeInfo == null && tag.instanceType != null) {
                attributeInfo = tagResolver.resolveAttributeAsProperty(
                    nameAsString,
                    tagExpr,
                    keyExpr,
                    valueExpr,
                    tag,
                    expectedTypes,
                    context
                )
            }

            if (attributeInfo == null) {
                // if we haven't resolved it to anything, check to see if it is an attribute consumed by a parameter of the resolved
                // tag function. If it is, it's a valid attribute, but it's "pivotal", meaning that when it changes, a new instance
                // will end up being created.
                val argDescriptor = attributeNameToDescriptor[nameAsString]

                if (argDescriptor != null) {
                    val argType = argDescriptor.type.let {
                        if(descriptorHasComposableChildrenAnnotation(argDescriptor))
                            R4aTypeResolutionInterceptorExtension.makeComposable(argDescriptor.module, it)
                        else it
                    }
                    attributeInfo = KtxAttributeInfo(
                        name = nameAsString,
                        isIncludedInConstruction = ctorAttributes.contains(nameAsString),
                        isPivotal = tag.isConstructed,
                        setterResolvedCall = null,
                        descriptor = argDescriptor,
                        type = argType
                    )
                    context.trace.record(
                        BindingContext.REFERENCE_TARGET,
                        keyExpr,
                        argDescriptor
                    )
                    facade.checkType(
                        valueExpr,
                        context.replaceExpectedType(argType)
                    )
                }
            }

            if (attributeInfo == null) {
                // error. We couldn't find this attribute, and it wasn't used in the main tag call, so it doesn't exist.
                val valueType = facade.getTypeInfo(valueExpr, context).type

                if (valueType == null) {
                    context.trace.report(
                        Errors.UNRESOLVED_REFERENCE.on(
                            keyExpr,
                            keyExpr
                        )
                    )
                } else if (expectedTypes.isEmpty()) {
                    context.trace.reportFromPlugin(
                        UNRESOLVED_ATTRIBUTE_KEY.on(
                            keyExpr,
                            tag.referrableDescriptor,
                            nameAsString,
                            valueType
                        ),
                        R4ADefaultErrorMessages
                    )
                } else {
                    expectedTypes.firstOrNull()?.let {
                        context.trace.record(BindingContext.EXPECTED_EXPRESSION_TYPE, valueExpr, it)
                    }

                    // TODO(lmr): It turns out that expectedTypes won't be populated with extension setters/properties that are in scope
                    // unless there are no other properties/setters with the same name. Ideally we would have these show up in the error
                    // message as well. Normal kotlin has the same issue, so I'm not going to spend too much time on this right now.
                    context.trace.reportFromPlugin(
                        R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                            keyExpr,
                            valueType,
                            expectedTypes
                        ),
                        R4ADefaultErrorMessages
                    )
                }
            } else {
                // success!
                context.trace.record(KTX_ATTR_INFO, attribute, attributeInfo)
                attributeInfos.add(attributeInfo)
            }
        }

        val composableType = getComposableType(tag.referrableDescriptor, context.scope.ownerDescriptor.module)

        val childrenInfo = tag.parameters.firstOrNull { it.isChildren }
        val childrenExpr = element.bodyLambdaExpression
        var childrenAttrInfo: KtxAttributeInfo? = null

        // process the children lambda
        if (childrenInfo == null) {
            // children wasn't used in the constructor. check to see if we have a children descriptor, and if we have children provided
            val childrenDescriptors = when (tag.referrableDescriptor) {
                is ClassDescriptor -> tag.referrableDescriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .filter { it.hasChildrenAnnotation() }
                is SimpleFunctionDescriptor -> tag.referrableDescriptor
                    .valueParameters
                    .filter { it.hasChildrenAnnotation() }
                else -> emptyList()
            }

            if (childrenExpr != null && childrenDescriptors.isEmpty()) {
                // user provided children, but none are declared.
                // check to see if it's an android view. if so, we allow children, but don't provide a childrenAttrInfo object.
                // we do need to make sure that the type system traverses the children though, so we handle that here:
                if (composableType == ComposableType.VIEW || composableType == ComposableType.EMITTABLE) {
                    val composableAnnotation = object : AnnotationDescriptor {
                        override val type: KotlinType get() = context.scope.ownerDescriptor.module.findClassAcrossModuleDependencies(ClassId.topLevel(R4aUtils.r4aFqName("Composable")))!!.defaultType
                        override val allValueArguments: Map<Name, ConstantValue<*>> get() = emptyMap()
                        override val source: SourceElement get() = SourceElement.NO_SOURCE
                        override fun toString() = "[@Composable]"
                    }
                    facade.checkType(
                        childrenExpr,
                        context.replaceExpectedType(
                            createFunctionType(
                                moduleDescriptor.builtIns,
                                Annotations.create(listOf(composableAnnotation)),
                                null,
                                emptyList(),
                                emptyList(),
                                moduleDescriptor.builtIns.unitType,
                                false
                            )
                        )
                    )
                } else {
                    context.trace.reportFromPlugin(
                        R4AErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(tagExpr),
                        R4ADefaultErrorMessages
                    )
                }
            }

            for (childrenDescriptor in childrenDescriptors) {
                if (childrenAttrInfo != null) {
                    // the first one to resolve correctly wins
                    continue
                }
                val isRequiredChildrenDescriptor = childrenDescriptor.isRequiredR4aAttribute(context.scope.ownerDescriptor)

                if (childrenExpr == null && isRequiredChildrenDescriptor) {
                    val name = childrenDescriptor.name.asString()
                    val namedAttribute = element.attributes.find { it.name == name }
                    if (namedAttribute == null) {
                        // there is a required children descriptor, but user didn't provide anything
                        context.trace.reportFromPlugin(
                            R4AErrors.MISSING_REQUIRED_CHILDREN.on(tagExpr),
                            R4ADefaultErrorMessages
                        )
                    }
                } else if (childrenExpr != null) {
                    when (childrenDescriptor) {
                        is SimpleFunctionDescriptor -> {
                            childrenAttrInfo = tagResolver.resolveChildrenAsSetter(
                                childrenDescriptor,
                                childrenExpr,
                                tag,
                                element,
                                context
                            )
                        }
                        is PropertyDescriptor -> {
                            childrenAttrInfo = tagResolver.resolveChildrenAsProperty(
                                childrenDescriptor,
                                childrenExpr,
                                tag,
                                element,
                                context
                            )
                        }
                        is ValueParameterDescriptor -> {
                            childrenAttrInfo = KtxAttributeInfo(
                                name = childrenDescriptor.name.asString(),
                                isPivotal = tag.isConstructed,
                                descriptor = childrenDescriptor,
                                type = childrenDescriptor.type,
                                setterResolvedCall = null,
                                isIncludedInConstruction = true
                            )
                        }
                        else -> error("unexpected descriptor annotated with @Children")
                    }
                }
            }

            if (childrenExpr != null && childrenAttrInfo == null && childrenDescriptors.isNotEmpty()) {
                // a children body is provided, but we couldn't resolve it to a @Children declaration on the component
                val potentialTypes = childrenDescriptors.mapNotNull { when (it) {
                    is SimpleFunctionDescriptor -> it.valueParameters.firstOrNull()?.type
                    is PropertyDescriptor -> it.type
                    else -> null
                }}
                context.trace.reportFromPlugin(
                    R4AErrors.UNRESOLVED_CHILDREN.on(tagExpr, potentialTypes),
                    R4ADefaultErrorMessages
                )
            }
        } else if (childrenExpr != null) {
            val childrenType = childrenInfo.descriptor.type.let {
                if(descriptorHasComposableChildrenAnnotation(childrenInfo.descriptor))
                    R4aTypeResolutionInterceptorExtension.makeComposable(childrenInfo.descriptor.module, it)
                else it
            }

            // children *was* used in the constructor, but the type system hasn't traversed down into it yet
            facade.checkType(
                childrenExpr,
                context.replaceExpectedType(childrenType)
            )
            childrenAttrInfo = KtxAttributeInfo(
                name = childrenInfo.name,
                descriptor = childrenInfo.descriptor,
                type = childrenType,
                isPivotal = !childrenInfo.descriptor.isVar,
                isIncludedInConstruction = true,
                setterResolvedCall = null
            )
        }

        // Most of the time, required attributes are part of the constructor or function call, so they are properly "found" in the
        // resolution of the tag. However, `lateinit` can be used on properties to have attributes that are "required", but not included
        // in the constructor. We want to find them explicitly here and make sure that we have them all:

        if (tag.isConstructed && tag.typeDescriptor != null) {
            val missingAttributes = mutableListOf<DeclarationDescriptor>()
            val descriptors = tag.typeDescriptor.unsubstitutedMemberScope.getContributedDescriptors()
            for (descriptor in descriptors) {
                val isRequired = descriptor.isRequiredR4aAttribute(context.scope.ownerDescriptor)
                if (isRequired && !attributeExpressions.containsKey(descriptor.name.asString()) && descriptor !== childrenAttrInfo?.descriptor) {
                    missingAttributes.add(descriptor)
                }
            }

            if (missingAttributes.size > 0) {
                context.trace.reportFromPlugin(
                    MISSING_REQUIRED_ATTRIBUTES.on(tagExpr, missingAttributes),
                    R4ADefaultErrorMessages
                )
            }
        }

        // TODO(lmr): validate the android specific stuff (component/view only) etc.
        // TODO(lmr): prevent jumps from occurring in attribute expressions
        // TODO(lmr): prevent return from happening inside of children, or attributes
        // TODO(lmr): handle annotations like @ConflictsWith(...)

        // We want to ensure that the attributes of the ktx tag are unique and there are no duplicates
        val duplicates = mutableListOf<KtxAttribute>()
        val set = HashSet<String>()
        for (attr in element.attributes) {
            val key = attr.key!!.getReferencedName()
            if (set.contains(key)) {
                duplicates.add(attr)
            } else {
                set.add(key)
            }
        }

        if (childrenAttrInfo != null && set.contains(childrenAttrInfo.name)) {
            // the duplicate attribute test won't catch situations where a children attribute is used as a ktx body lambda and also
            // referenced explicitly as an attribute, so we check for it here
            val attr = element.attributes.find { it.key!!.getReferencedName() == childrenAttrInfo.name }
            if (attr != null) {
                context.trace.reportFromPlugin(
                    R4AErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(attr.key!!, childrenAttrInfo.name),
                    R4ADefaultErrorMessages
                )
            }
        }

        for (dup in duplicates) {
            dup.key?.let {
                context.trace.reportFromPlugin(
                    R4AErrors.DUPLICATE_ATTRIBUTE.on(it),
                    R4ADefaultErrorMessages
                )
            }
        }

        val tagInfo = KtxTagInfo(
            composableType = composableType,
            isConstructed = tag.isConstructed,
            instanceType = tag.instanceType,
            resolvedCall = tag.resolvedCall,
            attributeInfos = attributeInfos,
            childrenInfo = childrenAttrInfo,
            parameterInfos = tag.parameters,
            referrableDescriptor = tag.referrableDescriptor
        )

        context.trace.record(KTX_TAG_INFO, element, tagInfo)
    }

    private fun DeclarationDescriptor.isRequiredR4aAttribute(ownerDescriptor: DeclarationDescriptor) = when (this) {
        is PropertyDescriptor -> isLateInit &&
                Visibilities.isVisibleIgnoringReceiver(this, ownerDescriptor) &&
                // Component unfortunately has some publicly exposed properties that will look like they are "required"
                // attributes, but we don't want people to use them as properties. We should figure out a better solution for
                // this, but for now I am just going to ignore them explicitly.
                overriddenDescriptors.firstOrNull()?.containingDeclaration?.fqNameSafe != R4aUtils.r4aFqName("Component")
        is ValueParameterDescriptor -> !declaresDefaultValue()
        is SimpleFunctionDescriptor -> false // setter function. never required
        else -> false
    }

    private fun fallback(
        element: KtxElement,
        context: ExpressionTypingContext,
        facade: ExpressionTypingFacade
    ) {
        // If we can't resolve the tag name, we can't accurately report errors on the element, but we still want the type system to traverse
        // through all of the children nodes so that specific errors on those can get recorded
        with(element) {
            attributes.forEach { facade.checkType(it, context) }
            body?.forEach { facade.checkType(it, context) }
        }
    }

    private fun getComposableType(tagDescriptor: DeclarationDescriptor, module: ModuleDescriptor): ComposableType {
        // TODO(lmr): cache this
        val r4aComponentId = ClassId.topLevel(R4aUtils.r4aFqName("Component"))
        val r4aComponentDescriptor = module.findClassAcrossModuleDependencies(r4aComponentId) ?: return ComposableType.UNKNOWN
        val r4aEmittableId = ClassId.topLevel(R4aUtils.r4aFqName("Emittable"))
        val r4aEmittableDescriptor = module.findClassAcrossModuleDependencies(r4aEmittableId) ?: return ComposableType.UNKNOWN

        // TODO(lmr): cache this
        // TODO(lmr): we should think about a non-android specific way to determine when we have hit a "node"
        val androidViewId = ClassId.topLevel(FqName("android.view.View"))
        val androidViewDescriptor = module.findClassAcrossModuleDependencies(androidViewId) ?: return ComposableType.UNKNOWN

        // TODO(lmr): I think this needs to be a bit more sophisticated
        return when (tagDescriptor) {
            is VariableDescriptor -> ComposableType.FUNCTION_VAR
            is SimpleFunctionDescriptor -> ComposableType.FUNCTION
            is ClassDescriptor -> {
                if (tagDescriptor.isSubclassOf(r4aComponentDescriptor)) ComposableType.COMPONENT
                else if (tagDescriptor.isSubclassOf(androidViewDescriptor)) ComposableType.VIEW
                else if (tagDescriptor.isSubclassOf(r4aEmittableDescriptor)) ComposableType.EMITTABLE
                else ComposableType.UNKNOWN
            }
            else -> ComposableType.UNKNOWN
        }
    }

    fun descriptorHasComposableChildrenAnnotation(descriptor: DeclarationDescriptor): Boolean {
        val annotation = descriptor.annotations.findAnnotation(ComposableAnnotationChecker.CHILDREN_ANNOTATION_NAME) ?: return false
        val composableValueArgument = annotation.argumentValue("composable")?.value
        return composableValueArgument == null || composableValueArgument == true
    }
}