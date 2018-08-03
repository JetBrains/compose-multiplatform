package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.analysis.R4AErrors.MISSING_REQUIRED_ATTRIBUTES
import org.jetbrains.kotlin.r4a.analysis.R4AErrors.UNRESOLVED_ATTRIBUTE_KEY
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.KTX_ATTR_INFO
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices.KTX_TAG_INFO
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.context.CallPosition
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade
import org.jetbrains.kotlin.types.isNullable


private fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(R4aUtils.r4aFqName("Children")) != null

class R4aKtxTypeResolutionExtension : KtxTypeResolutionExtension {

    override fun visitKtxElement(
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

        val tagExpr = element.simpleTagName ?: element.qualifiedTagName ?: return

        // TODO(lmr): we will need to provide the children lambda separately here most likely
        val attributeExpressions = element.attributes.map { it.key!!.getReferencedName() to (it.value ?: it.key!!) }.toMap()

        val tagResolver = KtxTagResolver(callResolver, facade, attributeExpressions, element.bodyLambdaExpression)

        val tag = tagResolver.resolveReference(tagExpr, context)

        if (tag == null) {
            // We couldn't find anything, so we mark things as unresolved
            element.simpleTagName?.let {
                context.trace.report(Errors.UNRESOLVED_REFERENCE.on(it, it))
            }
            element.simpleClosingTagName?.let {
                context.trace.report(Errors.UNRESOLVED_REFERENCE.on(it, it))
            }
            // if the tag is dot-qualified, we can't directly mark as unresolved, but the tagResolver handles each part of it, so we just
            // call it again on the closing tag, even though it's doing duplicate work from above and we know it won't resolve.
            element.qualifiedClosingTagName?.let {
                tagResolver.resolveReference(it, context)
            }
            // report an error on the entire element for good measure
            context.trace.reportFromPlugin(
                R4AErrors.UNRESOLVED_TAG.on(element),
                R4ADefaultErrorMessages
            )
            return
        }

        // If we've made it here, it resolved to something and our opening tag is marked but our closing tag is not, so we copy it over
        element.simpleClosingTagName?.let {
            val ref = context.trace[BindingContext.REFERENCE_TARGET, element.simpleTagName]
            context.trace.record(BindingContext.REFERENCE_TARGET, it, ref)
        }

        // again, if it's dot-qualified we have to do the more expensive thing of calling resolveReference(...) again so it marks
        // the closing tag properly.
        element.qualifiedClosingTagName?.let {
            tagResolver.resolveReference(it, context)
        }

        if (!tag.valid) {
            // we don't need to record any errors here since there will already be some on the trace from the failed resolve
            return
        }

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

            // we first look for "setFoo" methods to resolve the attribute to. This will include extension setter methods in scope.
            if (attributeInfo == null && tag.instanceType != null) {
                attributeInfo = tagResolver.resolveAttributeAsSetter(
                    nameAsString,
                    keyExpr,
                    valueExpr,
                    tag,
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
                    context
                )
            }

            if (attributeInfo == null) {
                // if we haven't resolved it to anything, check to see if it is an attribute consumed by a parameter of the resolved
                // tag function. If it is, it's a valid attribute, but it's "pivotal", meaning that when it changes, a new instance
                // will end up being created.
                val argDescriptor = attributeNameToDescriptor[nameAsString]

                if (argDescriptor != null) {
                    attributeInfo = KtxAttributeInfo(
                        name = nameAsString,
                        isIncludedInConstruction = ctorAttributes.contains(nameAsString),
                        isPivotal = tag.isConstructed,
                        setterResolvedCall = null,
                        descriptor = argDescriptor,
                        type = argDescriptor.type
                    )
                    context.trace.record(
                        BindingContext.REFERENCE_TARGET,
                        keyExpr,
                        argDescriptor
                    )
                    facade.getTypeInfo(
                        valueExpr,
                        context.replaceExpectedType(argDescriptor.type)
                    )
                }
            }

            if (attributeInfo == null) {
                // error. We couldn't find this attribute, and it wasn't used in the main tag call, so it doesn't exist.
                context.trace.report(
                    Errors.UNRESOLVED_REFERENCE.on(
                        keyExpr,
                        keyExpr
                    )
                )
                val valueType = facade.getTypeInfo(valueExpr, context).type ?: continue
                context.trace.reportFromPlugin(
                    UNRESOLVED_ATTRIBUTE_KEY.on(
                        attribute,
                        tag.referrableDescriptor,
                        nameAsString,
                        valueType
                    ),
                    R4ADefaultErrorMessages
                )
            } else if (attributeInfo.descriptor.hasChildrenAnnotation()) {
                // we do not allow children attributes to be passed in as normal attributes
                // TODO(lmr): report error saying that they should use ktx body for this attribute
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
            // TODO(lmr): for now we are just grabbing the first one, but I think in order to handle overloading properly we will need
            // to grab the list of all children annotated descriptors
            val childrenDescriptor = when (tag.referrableDescriptor) {
                is ClassDescriptor -> tag.referrableDescriptor
                    .unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .firstOrNull { it.hasChildrenAnnotation() }
                is SimpleFunctionDescriptor -> tag.referrableDescriptor
                    .valueParameters
                    .firstOrNull { it.hasChildrenAnnotation() }
                is VariableDescriptor -> tag.referrableDescriptor // TODO(lmr): not sure if this one is right....
                    .typeParameters
                    .firstOrNull { it.hasChildrenAnnotation() }
                else -> null
            }

            val isRequiredChildrenDescriptor = when (childrenDescriptor) {
                is PropertyDescriptor -> !childrenDescriptor.type.isNullable()
                is SimpleFunctionDescriptor -> false // setter function. never required
                is ValueParameterDescriptor -> !childrenDescriptor.declaresDefaultValue()
                is ParameterDescriptor -> true
                is TypeParameterDescriptor -> true
                else -> false
            }

            if (childrenExpr != null && childrenDescriptor == null) {
                // user provided children, but none are declared.
                // check to see if it's an android view. if so, we allow children, but don't provide a childrenAttrInfo object.
                // we do need to make sure that the type system traverses the children though, so we handle that here:
                if (composableType == ComposableType.VIEW) {
                    facade.getTypeInfo(
                        childrenExpr,
                        context.replaceExpectedType(
                            createFunctionType(
                                moduleDescriptor.builtIns,
                                Annotations.EMPTY,
                                null,
                                emptyList(),
                                emptyList(),
                                moduleDescriptor.builtIns.unitType,
                                false
                            )
                        )
                    )
                } else {
                    // TODO(lmr): mark tag with an error
                }
            } else if (childrenExpr == null && childrenDescriptor != null && isRequiredChildrenDescriptor) {
                // there is a required children descriptor, but user didn't provide anything
                // TODO(lmr): mark tag with an error
            } else if (childrenExpr != null && childrenDescriptor != null) {
                when (childrenDescriptor) {
                    is SimpleFunctionDescriptor -> {
                        // TODO(lmr): although we can resolve children as a setter function, it still doesn't show up as "used"
                        childrenAttrInfo = tagResolver.resolveAttributeAsSetter(
                            R4aUtils.propertyNameFromSetterMethod(childrenDescriptor.name.asString()),
                            tagExpr as KtReferenceExpression, // TODO(lmr): this won't always work
                            childrenExpr,
                            tag,
                            context
                        )
                    }
                    is PropertyDescriptor -> {
                        // TODO(lmr): we don't yet resolve children attributes as properties. working on it.
//                        tagResolver.resolveChildrenAsProperty(
//                            element,
//                            childrenDescriptor,
//                            childrenExpr,
//                            tag,
//                            context
//                        )
                    }
                    else -> error("unexpected descriptor annotated with @Children")
                }
            }
        } else if (childrenExpr != null) {
            // children *was* used in the constructor, but the type system hasn't traversed down into it yet
            facade.getTypeInfo(
                childrenExpr,
                context.replaceExpectedType(childrenInfo.descriptor.type)
            )
            childrenAttrInfo = KtxAttributeInfo(
                name = childrenInfo.name,
                descriptor = childrenInfo.descriptor,
                type = childrenInfo.descriptor.type,
                isPivotal = false,
                isIncludedInConstruction = false, // this only needs to be true for attributes that have a descriptor
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
                // TODO(lmr): do we need to check visibility here?
                val isRequired = when (descriptor) {
                    is PropertyDescriptor -> descriptor.isLateInit && !descriptor.hasChildrenAnnotation()
                    else -> false
                }
                if (isRequired && !attributeExpressions.containsKey(descriptor.name.asString())) {
                    missingAttributes.add(descriptor)
                }
            }

            if (missingAttributes.size > 0) {
                context.trace.reportFromPlugin(
                    MISSING_REQUIRED_ATTRIBUTES.on(element, missingAttributes),
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
            parameterInfos = tag.parameters
        )

        context.trace.record(KTX_TAG_INFO, element, tagInfo)
    }

    private fun getComposableType(tagDescriptor: DeclarationDescriptor, module: ModuleDescriptor): ComposableType {
        // TODO(lmr): cache this
        val r4aComponentId = ClassId.topLevel(R4aUtils.r4aFqName("Component"))
        val r4aComponentDescriptor = module.findClassAcrossModuleDependencies(r4aComponentId) ?: return ComposableType.UNKNOWN

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
                else ComposableType.UNKNOWN
            }
            else -> ComposableType.UNKNOWN
        }
    }
}