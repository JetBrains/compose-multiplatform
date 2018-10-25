package org.jetbrains.kotlin.r4a

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.ast.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingFacade
import org.jetbrains.kotlin.types.expressions.FakeCallResolver
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtxCallResolver(
    private val callResolver: CallResolver,
    private val facade: ExpressionTypingFacade,
    private val project: Project
) {
    private val fakeCallResolver = FakeCallResolver(project, callResolver)
    lateinit private var composerType: KotlinType
    lateinit private var composerResolvedCall: ResolvedCall<*>
    lateinit private var tagExpressions: List<KtExpression>

    private val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()
    private val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()

//    lateinit val simpleEmitMap: Map<KotlinType, ResolvedCall<*>>
//    lateinit val compoundEmitMap: Map<KotlinType, ResolvedCall<*>>
//    lateinit val statelessMemoize: ResolvedCall<*>?
//    lateinit val statefullMemoize: ResolvedCall<*>?
//    lateinit val joinKey: ResolvedCall<*>?

    private fun isStatic(expression: KtExpression, context: ExpressionTypingContext): Boolean {
        // TODO(lmr): make this smarter?  expression.isConstant() doesn't seem to work in testing?
        return false
    }

    /**
     *
     */
    fun findComposer(element: KtxElement, context: ExpressionTypingContext): Boolean {

        tagExpressions = listOfNotNull(
            element.simpleTagName,
            element.simpleClosingTagName,
            element.qualifiedTagName,
            element.qualifiedClosingTagName
        )

        val resolvedComposer = resolveVar(COMPOSER_NAME, element, context)
        if (!resolvedComposer.isSuccess) {
            // TODO(lmr): diagnostic
            return false
        }

        composerResolvedCall = resolvedComposer.resultingCall

        val descriptor = composerResolvedCall.resultingDescriptor

        composerType = when (descriptor) {
            is PropertyDescriptor -> descriptor.type
            is VariableDescriptor -> descriptor.type
            else -> return false
        }

        val composerTypeDescriptor = composerType.constructor.declarationDescriptor as? ClassDescriptor ?: return false

        for (memDescriptor in composerTypeDescriptor.unsubstitutedMemberScope.getContributedDescriptors { it == EMIT_NAME }) {
            recordComposerEmitBounds(memDescriptor)
        }

        return true
    }

    private fun KotlinType.upperBounds(): List<KotlinType> {
        return if (isTypeParameter()) {
            TypeUtils.getTypeParameterDescriptorOrNull(this)?.upperBounds ?: emptyList()
        } else {
            listOf(this)
        }
    }

    private fun KotlinType.isEmittable(compound: Boolean = false): Boolean {
        val upperBounds = if (compound) emitCompoundUpperBoundTypes else emitSimpleUpperBoundTypes
        return upperBounds.any { isSubtypeOf(it) }
    }

    private fun KotlinType.isCompoundEmittable(): Boolean {
        return emitCompoundUpperBoundTypes.any { isSubtypeOf(it) }
    }


    private fun recordComposerEmitBounds(descriptor: DeclarationDescriptor) {
        if (descriptor.name != EMIT_NAME) return
        if (descriptor !is SimpleFunctionDescriptor) return
        with (descriptor) {
            // TODO(lmr): we could report diagnostics on some of these? seems strange to though...
            if (valueParameters.size < 3) return
            if (valueParameters.size > 4) return
            val ctorParam = valueParameters.find { it.name == CTOR_PARAMETER_NAME } ?: return
            if (!ctorParam.type.isFunctionTypeOrSubtype) return
            val upperBounds = ctorParam.type.getReturnTypeFromFunctionType().upperBounds()

            // TODO(lmr): grab any parameters in the type definition here that we will use as implicit attributes (ie, "context")

            emitSimpleUpperBoundTypes.addAll(upperBounds)

            if(valueParameters.any { it.name == CHILDREN_PARAMETER_NAME }) {
                emitCompoundUpperBoundTypes.addAll(upperBounds)
            }
        }
    }

    private sealed class ResolveKind {
        object ROOT : ResolveKind()
        class NESTED(val calleeType: KotlinType) : ResolveKind()
    }

    private class AttributeInfo(
        val value: KtExpression,
        val key: KtSimpleNameExpression?,
        val name: String,
        val isChildren: Boolean
    ) {
        constructor(attr: KtxAttribute) : this(attr.value ?: attr.key!!, attr.key, attr.name!!, false)
        constructor(body: KtLambdaExpression) : this(body, null, CHILDREN_KEY, true)

        fun makeArgumentValue(name: String = this.name): ValueArgument {
            val argumentName = object : ValueArgumentName {
                override val asName: Name
                    get() = Name.identifier(name)
                override val referenceExpression: KtSimpleNameExpression?
                    get() = key
            }
            return object : ValueArgument {
                override fun getArgumentExpression() = value
                override fun getArgumentName() = argumentName
                override fun isNamed() = true
                override fun asElement(): KtElement = value
                override fun getSpreadElement() = null
                override fun isExternal() = true
            }
        }
    }

    /**
     *
     */
    fun resolveTag(
        tagExpressions: List<KtExpression>,
        attributes: Collection<KtxAttribute>,
        body: KtLambdaExpression?,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall? {
        val openTagExpr = tagExpressions.first()
        val receiver = resolveReceiver(openTagExpr, context)

        val attrInfos = mutableMapOf<String, AttributeInfo>()
        for (attr in attributes) {
            AttributeInfo(attr).let { attrInfos.put(it.name, it) }
        }
        body?.let { AttributeInfo(it) }?.let { attrInfos.put(it.name, it) }

        val usedAttributes = mutableSetOf<String>()

        val call = makeCall(
            openTagExpr,
            receiver = receiver,
            calleeExpression = when (openTagExpr) {
                is KtQualifiedExpression -> openTagExpr.selectorExpression
                is KtSimpleNameExpression -> openTagExpr
                else -> return null
            }
        )

        val emitOrCall = resolveThing(
            openTagExpr,
            ResolveKind.ROOT,
            call,
            attrInfos,
            usedAttributes,
            context
        )

        // TODO(lmr): validate that if it bottoms out at an emit(...) that it doesn't have any call(...)s

        if (emitOrCall != null) {
            return ResolvedKtxElementCall(
                usedAttributes = emptyList(),
                unusedAttributes = emptyList(),
                emitOrCall = emitOrCall
            )
        }

        return null


        // TODO(lmr): do we need to check the type that it bottoms out at?
//        val validTagType = isValidTagType(instanceType, context)

//        if (instanceType != null && !validTagType) {
//            temporaryForVariable.trace.reportFromPlugin(
//                R4AErrors.INVALID_TAG_TYPE.on(
//                    expression,
//                    instanceType,
//                    possibleTagTypes(context)
//                ),
//                R4ADefaultErrorMessages
//            )
//        }

    }

    private fun forceResolveCallForInvoke(
        calleeType: KotlinType,
        context: BasicCallResolutionContext
    ) : OverloadResolutionResults<FunctionDescriptor> {
        val calleeExpression = context.call.calleeExpression ?: error("callee expression required on call")
        val expressionReceiver = ExpressionReceiver.create(calleeExpression, calleeType, context.trace.bindingContext)
        val call = CallTransformer.CallForImplicitInvoke(
            context.call.explicitReceiver, expressionReceiver, context.call,
            false
        )
        val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, call, calleeType)
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
    }

    private fun resolveCallForInvoke(
        context: BasicCallResolutionContext,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        return callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            context, OperatorNameConventions.INVOKE, tracing,
            NewResolutionOldInference.ResolutionKind.Invoke
        )
    }

    private fun resolveThing(
        expression: KtExpression,
        kind: ResolveKind,
        call: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        context: ExpressionTypingContext
    ): EmitOrCallNode? {
        val tmpForCandidates = TemporaryTraceAndCache.create(
            context, "trace to resolve ktx element", expression
        )
        val results = getCandidates(kind, call, context.replaceTraceAndCache(tmpForCandidates))

        // TODO(lmr): we could have an optimization for results.isSuccess and attributes.size == 0 here

        class TempResolveInfo(
            val valid: Boolean,
            val trace: TemporaryTraceAndCache,
            val attributesLeft: Int,
            val build: () -> EmitOrCallNode
        )

        val resolveInfos = results.resultingCalls.mapNotNull { result ->
            val tmpForCandidate = TemporaryTraceAndCache.create(
                context, "trace to resolve ktx element", expression
            )

            val candidateContext = context.replaceTraceAndCache(tmpForCandidate)

            val attrsUsedInCall = mutableSetOf<String>()

            val resolvedCall = resolveCandidate(
                kind,
                result,
                call,
                attributes,
                attrsUsedInCall,
                candidateContext
            )

            if (resolvedCall == null) return@mapNotNull null

            val returnType = resolvedCall.resultingDescriptor.returnType


            if (returnType == null || returnType.isUnit()) {
                // bottomed out
                // TODO(lmr): there are cases where this should be a memoized call though!
                return@mapNotNull TempResolveInfo(
                    true, // TODO(lmr): valid
                    tmpForCandidate,
                    attributes.size - attrsUsedInCall.size
                ) {
                    NonMemoizedCallNode(
                        resolvedCall = resolvedCall,
                        params = constructAttributeNodes(resolvedCall, attributes)
                    )
                }
            }


            val attrsUsedInSets = mutableSetOf<String>()

            val setterValidations = resolveAllSetAttributes(
                returnType,
                attributes,
                attrsUsedInCall,
                attrsUsedInSets,
                candidateContext
            )

            val pivotals = resolvePivotalAttributes(
                resolvedCall,
                attributes,
                attrsUsedInCall,
                attrsUsedInSets,
                returnType
            )


            if (returnType.isEmittable()) {

                return@mapNotNull TempResolveInfo(
                    true,
                    tmpForCandidate,
                    attributes.size - attrsUsedInCall.size
                ) {
                    EmitCallNode(
                        memoize = ComposerCallInfo(
                            pivotals = emptyList(), // TODO(lmr):
                            joinKeyCall = null, // TODO(lmr):
                            ctorCall = resolvedCall,
                            ctorParams = constructCtorValueNodes(resolvedCall, attributes),
                            staticAssignments = emptyList(), // TODO(lmr):
                            validations = setterValidations
                        )
                    )
                }
            }

            val attrsUsedInFollowingCalls = mutableSetOf<String>()
            // TODO(lmr): the validations from the child need to be added / copied to the setter validations
            // and marked as CHANGED

            // TODO(lmr): prevent recursion if we have not consumed 1 or more attributes....
            val childCall = resolveThing(
                expression,
                ResolveKind.NESTED(returnType),
                makeCall(
                    call.callElement,
                    calleeExpression = call.calleeExpression,
                    dispatchReceiver = TransientReceiver(returnType)
                ),
                attributes - attrsUsedInCall - attrsUsedInSets,
                attrsUsedInFollowingCalls,
                candidateContext
            )

            // TODO(lmr): is canceling early here the right thing to do? what about in the case of emit(...)?
            // TODO(lmr): if childcall returned null, we should add an "INVALID_TAG_TYPE" diagnostic...
            // TODO(lmr): we probably shouldn't let childCall return null? makes error reporting harder....
            if (childCall == null) return@mapNotNull null

            val attrsLeft = attributes.size - attrsUsedInCall.size - attrsUsedInSets.size - attrsUsedInFollowingCalls.size

            // TODO(lmr): if we have 0 attributes left and it's a success, we can go ahead and return early here

            return@mapNotNull TempResolveInfo(
                true, // TODO(lmr): valid
                tmpForCandidate,
                attrsLeft
            ) {
                MemoizedCallNode(
                    memoize = ComposerCallInfo(
                        pivotals = emptyList(), // TODO(lmr):
                        joinKeyCall = null, // TODO(lmr):
                        ctorCall = resolvedCall,
                        ctorParams = constructAttributeNodes(resolvedCall, attributes),
                        staticAssignments = emptyList(), // TODO(lmr):
                        validations = setterValidations + childCall.consumedAttributes().toSet().map { it.asChangedValidatedAssignment() }
                    ),
                    call = childCall
                )
            }
        }
            .sortedWith(Comparator { a, b ->
                if (a.attributesLeft != b.attributesLeft) {
                    a.attributesLeft - b.attributesLeft
                } else {
                    (if (a.valid) 0 else 1) - (if (b.valid) 0 else 1)
                }
            })

        val result = resolveInfos.firstOrNull()

        if (result == null) {
            // TODO(lmr): no possible call found... return null?
            return null
        } else {
            result.trace.commit()
            // TODO(lmr): should we check to see if another resolveInfo has same # of attributes used or something?
            val resultNode = result.build()
            usedAttributes.addAll(resultNode.consumedAttributes().map { it.name })
            return resultNode
        }
    }

    private fun resolvePivotalAttributes(
        resolvedCall: ResolvedCall<*>,
        attributes: Map<String, AttributeInfo>,
        attrsUsedInCall: Set<String>,
        attrsUsedInSets: Set<String>,
        returnType: KotlinType?
    ): Set<String> {
        val fnDescriptor = resolvedCall.resultingDescriptor

        // TODO(lmr): this isn't quite right... what if a children param is annotated with @Pivotal?
        if (returnType == null || returnType.isUnit()) {
            return fnDescriptor
                .valueParameters
                .filter { isAnnotatedAsPivotal(it) }
                .map { it.name.asString() }
                .toSet()
        }

        val clsDescriptor = returnType.constructor.declarationDescriptor as? ClassDescriptor ?: return emptySet()
        val members = clsDescriptor
            .unsubstitutedMemberScope
            .getContributedDescriptors()
            .mapNotNull {
                when (it) {
                    is SimpleFunctionDescriptor -> if (R4aUtils.isSetterMethodName(it.name.asString())) it
                        else null
                    is PropertyDescriptor -> it
                    else -> null
                }
            }
        // TODO(lmr):
        return emptySet()
    }

    private fun constructAttributeNodes(resolvedCall: ResolvedCall<*>, attributes: Map<String, AttributeInfo>): List<AttributeNode> {
        return resolvedCall.resultingDescriptor.valueParameters.mapNotNull { param ->
            val name = param.name.asString()
            var attr = attributes[name]

            if (isChildrenParameter(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
            }

            if (attr == null) {
                // TODO(lmr): remove this eventually, but right now I want to know why attr is null
                error("why is attr null")
//                return@mapNotNull null
            }

            AttributeNode(
                name = name,
                isStatic = false,
                descriptor = param,
                type = param.type,
                expression = attr.value
            )
        }
    }

    private fun constructCtorValueNodes(resolvedCall: ResolvedCall<*>, attributes: Map<String, AttributeInfo>): List<ValueNode> {
        return resolvedCall.resultingDescriptor.valueParameters.mapNotNull { param ->
            val name = param.name.asString()
            var attr = attributes[name]

            if (isChildrenParameter(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
            }

            if (attr == null && isContextParameter(param)) {
                return@mapNotNull ImplicitCtorValueNode(
                    name = CONTEXT_KEY,
                    descriptor = param,
                    type = param.type
                )
            }

            if (attr == null) {
                // TODO(lmr): remove this eventually, but right now I want to know why attr is null
                error("why is attr null")
//                return@mapNotNull null
            }

            AttributeNode(
                name = name,
                isStatic = false,
                descriptor = param,
                type = param.type,
                expression = attr.value
            )
        }
    }

    private fun AttributeNode.asChangedValidatedAssignment(): ValidatedAssignment {
        return ValidatedAssignment(
            validationType = ValidationType.CHANGED,
            validationCall = null, // TODO(lmr):
            attribute = this,
            assignment = null
        )
    }

    private fun resolveAllSetAttributes(
        type: KotlinType,
        attributes: Map<String, AttributeInfo>,
        attributesUsedInCall: Set<String>,
        consumedAttributes: MutableSet<String>,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        val results = mutableListOf<ValidatedAssignment>()
        var children: AttributeInfo? = null
        for ((name, attribute) in attributes) {
            if (name == CHILDREN_KEY) {
                children = attribute
                continue
            }
            val keyExpr = attribute.key ?: error("key expected")

            val expectedTypes = mutableListOf<KotlinType>()

            var resolvedCall: ResolvedCall<*>? = null

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsSetter(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context
                )
            }

            if (resolvedCall == null) {
                resolvedCall = resolveAttributeAsProperty(
                    type,
                    attribute.name,
                    keyExpr,
                    attribute.value,
                    expectedTypes,
                    context
                )
            }

            if (resolvedCall != null) {

                results.add(ValidatedAssignment(
                    validationType = when {
                        attributesUsedInCall.contains(name) -> ValidationType.UPDATE
                        else -> ValidationType.SET
                    },
                    assignment = resolvedCall,
                    attribute = AttributeNode(
                        name = name,
                        expression = attribute.value,
                        type = resolvedCall.resultingDescriptor.valueParameters.first().type,
                        descriptor = resolvedCall.resultingDescriptor,
                        isStatic = false /* attribute.value.isConstant() */
                    ),
                    validationCall = null // TODO(lmr)
                ))
                consumedAttributes.add(name)
            }
        }

        if (children != null) {
            val expectedTypes = mutableListOf<KotlinType>()

            val childrenExpr = children.value as KtxLambdaExpression

            var resolvedCall: ResolvedCall<*>? = null

            for (descriptor in getChildrenDescriptors(type)) {
                if (resolvedCall != null) break

                when (descriptor) {
                    is PropertyDescriptor -> {
                        resolvedCall = resolveChildrenAsProperty(
                            type,
                            descriptor,
                            childrenExpr,
                            expectedTypes,
                            context
                        )
                    }
                    is SimpleFunctionDescriptor -> {
                        resolvedCall = resolveChildrenAsSetter(
                            type,
                            descriptor,
                            childrenExpr,
                            expectedTypes,
                            context
                        )
                    }
                }
            }
            if (resolvedCall != null) {
                results.add(ValidatedAssignment(
                    validationType = when {
                        attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                        else -> ValidationType.SET
                    },
                    assignment = resolvedCall,
                    attribute = AttributeNode(
                        name = CHILDREN_KEY,
                        expression = children.value,
                        type = resolvedCall.resultingDescriptor.valueParameters.first().type,
                        descriptor = resolvedCall.resultingDescriptor,
                        isStatic = false /* children.value.isConstant() */
                    ),
                    validationCall = null // TODO(lmr)
                ))
                consumedAttributes.add(CHILDREN_KEY)
            }
        }
        return results
    }

    private fun getChildrenDescriptors(type: KotlinType): List<DeclarationDescriptor> {
        val descriptor = type.constructor.declarationDescriptor
        return when (descriptor) {
            is ClassDescriptor -> descriptor
                .unsubstitutedMemberScope
                .getContributedDescriptors()
                .filter { it.hasChildrenAnnotation() }
            else -> emptyList()
        }
    }

    private fun resolveAttributeAsSetter(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtReferenceExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ) : ResolvedCall<*>? {
        val setterName = Name.identifier(R4aUtils.setterMethodFromPropertyName(name))
        val ambiguousReferences = mutableSetOf<DeclarationDescriptor>()

        if (valueExpr === keyExpr) {
            // punning...
            // punning has a single expression that both acts as reference to the value and to the property/setter. As a result, we do
            // two separate resolution steps, but we need to use BindingContext.AMBIGUOUS_REFERENCE_TARGET instead of
            // BindingContext.REFERENCE_TARGET, and since we can't unset the latter, we have to retrieve it from a temporary trace
            // and manually set the references later. Here we resolve the "reference to the value" and save it:
            val temporaryForPunning = TemporaryTraceAndCache.create(
                context, "trace to resolve reference for punning", keyExpr
            )

            facade.getTypeInfo(
                keyExpr,
                context.replaceTraceAndCache(temporaryForPunning)
            )

            temporaryForPunning.trace[BindingContext.REFERENCE_TARGET, keyExpr]?.let {
                ambiguousReferences.add(it)
            }

            temporaryForPunning.commit()
        }

        val receiver = TransientReceiver(instanceType)

        val call = makeCall(
            keyExpr,
            calleeExpression = keyExpr,
            valueArguments = listOf(CallMaker.makeValueArgument(valueExpr)),
            receiver = receiver
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", keyExpr
        )

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            keyExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (results.isAmbiguity || temporaryForFunction.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.addAll(
                results.resultingCalls.mapNotNull { it.resultingDescriptor.valueParameters.firstOrNull() }.map { it.type }
            )
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        if (valueExpr === keyExpr) {
            // punning...
            // we want to commit this trace, but filter out any REFERENCE_TARGET traces
            temporaryForFunction.trace.commit(
                { slice, value ->
                    !(value === valueExpr && (slice === BindingContext.REFERENCE_TARGET || slice === BindingContext.CALL))
                }, false
            )
            // TODO(lmr): even w/ ambiguous reference target, because we are setting a real reference target (which we really need to do
            // for codegen), the target of the actual descriptor doesn't show up...
            temporaryForFunction.cache.commit()
            ambiguousReferences.add(resolvedCall.resultingDescriptor)
            context.trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, keyExpr, ambiguousReferences)
        } else {
            // if we weren't punning, we can just commit like normal
            temporaryForFunction.commit()
        }

        return resolvedCall
    }

    private fun resolveAttributeAsProperty(
        instanceType: KotlinType,
        name: String,
        keyExpr: KtSimpleNameExpression,
        valueExpr: KtExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ) : ResolvedCall<*>? {
        val ambiguousReferences = mutableSetOf<DeclarationDescriptor>()

        if (valueExpr === keyExpr) {
            // punning...
            // punning has a single expression that both acts as reference to the value and to the property/setter. As a result, we do
            // two separate resolution steps, but we need to use BindingContext.AMBIGUOUS_REFERENCE_TARGET instead of
            // BindingContext.REFERENCE_TARGET, and since we can't unset the latter, we have to retrieve it from a temporary trace
            // and manually set the references later. Here we resolve the "reference to the value" and save it:
            val temporaryForPunning = TemporaryTraceAndCache.create(
                context, "trace to resolve reference for punning", keyExpr
            )

            facade.getTypeInfo(
                keyExpr,
                context.replaceTraceAndCache(temporaryForPunning)
            )

            temporaryForPunning.trace[BindingContext.REFERENCE_TARGET, keyExpr]?.let {
                ambiguousReferences.add(it)
            }
            temporaryForPunning.commit()
        }

        // NOTE(lmr): I'm not sure what the consequences are of using the tagExpr as the receiver...
        val receiver = TransientReceiver(instanceType)

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", keyExpr
        )

        val call = CallMaker.makePropertyCall(receiver, null, keyExpr)

        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.resolveSimpleProperty(contextForVariable)

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        val expectedType = (resolvedCall.resultingDescriptor as PropertyDescriptor).type

        facade.getTypeInfo(
            valueExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType(expectedType)
                .replaceCallPosition(CallPosition.PropertyAssignment(keyExpr))
        )

        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            expectedTypes.add(expectedType)
            return null
        }

        val descriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        val setter = descriptor.setter ?: return null

        // NOTE(lmr): Without this, the value arguments don't seem to end up in the resolved call. I'm not
        // sure if there is a better way to do this or not but this seems to work okay.
        val setterCall = makeCall(
            resolvedCall.call.callElement,
            calleeExpression = resolvedCall.call.calleeExpression,
            receiver = resolvedCall.call.explicitReceiver,
            valueArguments = listOf(CallMaker.makeValueArgument(valueExpr))
        )

        val resolutionCandidate = ResolutionCandidate.create(
            setterCall, setter, resolvedCall.dispatchReceiver, resolvedCall.explicitReceiverKind, null
        )

        val resolvedSetterCall = ResolvedCallImpl.create(
            resolutionCandidate,
            TemporaryBindingTrace.create(context.trace, "Trace for fake property setter resolved call"),
            TracingStrategy.EMPTY,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, setterCall)
        )

        setterCall.valueArguments.forEachIndexed { index, arg ->
            resolvedSetterCall.recordValueArgument(
                setter.valueParameters[index],
                ExpressionValueArgument(arg)
            )
        }

        resolvedSetterCall.markCallAsCompleted()

        if (valueExpr === keyExpr) {
            // punning...
            temporaryForVariable.trace.commit(
                { slice, value ->
                    !(value === valueExpr && (slice === BindingContext.REFERENCE_TARGET || slice === BindingContext.CALL))
                }, false
            )
            temporaryForVariable.cache.commit()
            ambiguousReferences.add(descriptor)
            // TODO(lmr): even w/ ambiguous reference target, because we are setting a real reference target (which we really need to do
            // for codegen), the target of the actual descriptor doesn't show up...
            context.trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, keyExpr, ambiguousReferences)
        } else {
            temporaryForVariable.commit()
        }

        return resolvedSetterCall
    }

    private fun resolveChildrenAsSetter(
        instanceType: KotlinType,
        childrenDescriptor: SimpleFunctionDescriptor,
        childrenExpr: KtxLambdaExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        // TODO(lmr): should we use FakeCallResolver here???

        val setterName = childrenDescriptor.name

        val valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr))
        val receiver = TransientReceiver(instanceType)
        val call = makeCall(
            childrenExpr,
            valueArguments = valueArguments,
            receiver = receiver,
            calleeExpression = childrenExpr // NOTE(lmr): this seems wrong
        )

        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve as function call", childrenExpr
        )

        val results = callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            setterName,
            childrenExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (temporaryForFunction.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }
        // TODO(lmr): should we check isSuccess here or anything like that?

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        temporaryForFunction.commit()

        return resolvedCall

//        return KtxAttributeInfo(
//            name = name,
//            type = resolvedCall.resultingDescriptor.valueParameters[0].type,
//            descriptor = resolvedCall.resultingDescriptor,
//            setterResolvedCall = resolvedCall,
//            isIncludedInConstruction = attributesInConstruction.contains(name),
//            isPivotal = false
//        )
    }

    private fun resolveChildrenAsProperty(
        instanceType: KotlinType,
        propertyDescriptor: PropertyDescriptor,
        childrenExpr: KtxLambdaExpression,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", childrenExpr
        )

        val receiver = TransientReceiver(instanceType)
        val call = makeCall(
            childrenExpr,
            calleeExpression = childrenExpr,
            receiver = receiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )

        val results = callResolver.computeTasksAndResolveCall<PropertyDescriptor>(
            contextForVariable,
            propertyDescriptor.name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )

        if (results.isNothing) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        facade.getTypeInfo(
            childrenExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType((resolvedCall.resultingDescriptor).type)
                .replaceCallPosition(CallPosition.PropertyAssignment(null))
        )
        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }
        val descriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        val setter = descriptor.setter ?: return null

        // NOTE(lmr): Without this, the value arguments don't seem to end up in the resolved call. I'm not
        // sure if there is a better way to do this or not but this seems to work okay.
        val setterCall = makeCall(
            resolvedCall.call.callElement,
            calleeExpression = resolvedCall.call.calleeExpression,
            receiver = resolvedCall.call.explicitReceiver,
            valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr)) // TODO(lmr): check to see if adding this above to value arguments fixes it?????
        )

        val resolutionCandidate = ResolutionCandidate.create(
            setterCall, setter, resolvedCall.dispatchReceiver, resolvedCall.explicitReceiverKind, null
        )

        val resolvedSetterCall = ResolvedCallImpl.create(
            resolutionCandidate,
            TemporaryBindingTrace.create(context.trace, "Trace for fake property setter resolved call"),
            TracingStrategy.EMPTY,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, setterCall)
        )

        setterCall.valueArguments.forEachIndexed { index, arg ->
            resolvedSetterCall.recordValueArgument(
                setter.valueParameters[index],
                ExpressionValueArgument(arg)
            )
        }

        resolvedSetterCall.markCallAsCompleted()

        temporaryForVariable.commit()


        return resolvedSetterCall

//        return KtxAttributeInfo(
//            name = descriptor.name.asString(),
//            type = descriptor.type,
//            descriptor = descriptor,
//            setterResolvedCall = resolvedSetterCall,
//            isIncludedInConstruction = false, // attributesInConstruction.contains(nameAsString),
//            isPivotal = !descriptor.isVar // NOTE(lmr): i don't think this can happen... it wouldn't resolve here in this case
//        )
    }

    private fun resolveCandidate(
        kind: ResolveKind,
        candidate: ResolvedCall<*>,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val valueArguments = mutableListOf<ValueArgument>()

        val referencedDescriptor = candidate.resultingDescriptor

        for (param in referencedDescriptor.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && isChildrenParameter(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)
                    arg = childrenAttr.makeArgumentValue(name)
                }
            }

            if (arg == null && attr != null) {
                usedAttributes.add(name)
                context.trace.record(BindingContext.REFERENCE_TARGET, attr.key, param)
                arg = attr.makeArgumentValue()
            }

            if (arg == null && isContextParameter(param)) {
                arg = CONTEXT_ARGUMENT
            }

            if (arg != null) {
                valueArguments.add(arg)
            }
        }

        val call = makeCall(
            original.callElement,
            valueArguments = valueArguments,
            calleeExpression = original.calleeExpression,
            receiver = original.explicitReceiver
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        val result = when (kind) {
            is ResolveKind.ROOT -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveKind.NESTED -> forceResolveCallForInvoke(kind.calleeType, contextForVariable)
        }

        // TODO(lmr): I think we want to return a result no matter what here?
        if (!result.isSuccess) return null
        return result.resultingCall
    }

    private fun getCandidates(
        kind: ResolveKind,
        call: Call,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val contextForVariable = BasicCallResolutionContext.create(
            context,
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        return when (kind) {
            is ResolveKind.ROOT -> callResolver.resolveFunctionCall(contextForVariable)
            is ResolveKind.NESTED -> forceResolveCallForInvoke(kind.calleeType, contextForVariable)
        }
    }

    private fun resolveReceiver(expression: KtExpression, context: ExpressionTypingContext): Receiver? {
        if (expression !is KtQualifiedExpression) return null
        val currentContext = context
            .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        expression.elementChain(currentContext)

        val receiverExpr = expression.receiverExpression

        val receiverTypeInfo = when (context.trace.get(BindingContext.QUALIFIER, receiverExpr)) {
            null -> facade.getTypeInfo(receiverExpr, currentContext)
            else -> KotlinTypeInfo(null, currentContext.dataFlowInfo)
        }

        // TODO(lmr): inspect jumps and nullability. We cant allow tags that can be null or return early
        val receiverType = receiverTypeInfo.type
            ?: ErrorUtils.createErrorType("Type for " + receiverExpr.text)

        return context.trace.get(BindingContext.QUALIFIER, receiverExpr)
            ?: ExpressionReceiver.create(receiverExpr, receiverType, context.trace.bindingContext)
    }

    companion object {
        private val CONTEXT_ARGUMENT = object : ValueArgument {
            override fun getArgumentExpression(): KtExpression? = null
            override fun getArgumentName(): ValueArgumentName? = null
            override fun isNamed(): Boolean = false
            override fun asElement(): KtElement = error("tried to get element")
            override fun getSpreadElement(): LeafPsiElement? = null
            override fun isExternal(): Boolean = true
        }

        private fun isContextParameter(it: ValueParameterDescriptor): Boolean {
            // TODO(lmr): need better approach for this
            return it.type.constructor.declarationDescriptor?.fqNameSafe == CONTEXT_FQNAME
        }

        private fun isChildrenParameter(it: ValueParameterDescriptor): Boolean {
            return it.annotations.findAnnotation(CHILDREN_FQNAME) != null
        }

        private fun isAnnotatedAsPivotal(it: Annotated): Boolean {
            return it.annotations.findAnnotation(PIVOTAL_FQNAME) != null
        }

        private fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(CHILDREN_FQNAME) != null

        private val CHILDREN_KEY = "<children>"
        private val CONTEXT_KEY = "<context>"

        private val COMPOSABLE_FQNAME = R4aUtils.r4aFqName("Composable")
        private val PIVOTAL_FQNAME = R4aUtils.r4aFqName("Pivotal")
        private val CHILDREN_FQNAME = R4aUtils.r4aFqName("Children")
        private val EMITTABLE_FQNAME = R4aUtils.r4aFqName("Emittable")
        private val CONTEXT_FQNAME = FqName("android.content.Context")

        private val COMPOSER_NAME = Name.identifier("composer")
        private val EMIT_NAME = Name.identifier("emit")
        private val MEMOIZE_NAME = Name.identifier("memoize")
        private val JOINKEY_NAME = Name.identifier("joinKey")
        private val CTOR_PARAMETER_NAME = Name.identifier("ctor")
        private val CHILDREN_PARAMETER_NAME = Name.identifier("children")
    }
/*
    private fun makeComposerCall(
        callExpression: KtExpression,
        callElement: KtElement,
        context: ExpressionTypingContext
    ) = makeCall(
        callElement = callElement,
        calleeExpression = callExpression,
        receiver = ExpressionReceiver.create(callExpression, composerType, context.trace.bindingContext)
    )
*/
    private fun makeCall(
        callElement: KtElement,
        calleeExpression: KtExpression? = null,
        valueArguments: List<ValueArgument> = emptyList(),
        receiver: Receiver? = null,
        dispatchReceiver: ReceiverValue? = null
    ): Call {
        return object : Call {
            override fun getDispatchReceiver(): ReceiverValue? = dispatchReceiver
            override fun getValueArgumentList(): KtValueArgumentList? = null
            override fun getTypeArgumentList(): KtTypeArgumentList? = null
            override fun getExplicitReceiver(): Receiver? = receiver
            override fun getCalleeExpression(): KtExpression? = calleeExpression
            override fun getValueArguments(): List<ValueArgument> = valueArguments
            override fun getCallElement(): KtElement = callElement
            override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
            override fun getTypeArguments(): List<KtTypeProjection> = emptyList()
            override fun getCallType(): Call.CallType = Call.CallType.DEFAULT
            override fun getCallOperationNode(): ASTNode? = null
        }
    }

    private fun resolveVar(
        name: Name,
        expr: KtExpression,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<CallableDescriptor> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expr
        )
        val call = makeCall(expr)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )
        return callResolver.computeTasksAndResolveCall<CallableDescriptor>(
            contextForVariable,
            name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Variable
        )
    }
/*
    private fun resolveMethod(
        type: KotlinType,
        name: Name,
        tagExpr: KtExpression,
        element: KtxElement,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolve composer", tagExpr
        )
        val call = makeComposerCall(tagExpr, element, context)
        return callResolver.computeTasksAndResolveCall<FunctionDescriptor>(
            BasicCallResolutionContext.create(
                context.replaceTraceAndCache(temporaryForFunction),
                call,
                CheckArgumentTypesMode.CHECK_CALLABLE_TYPE,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            name,
            TracingStrategy.EMPTY,
            NewResolutionOldInference.ResolutionKind.Function
        )
    }
*/



    private fun KtQualifiedExpression.elementChain(context: ExpressionTypingContext) {
        val moduleDescriptor = context.scope.ownerDescriptor.module
        val trace = context.trace
        val scopeForFirstPart = context.scope

        val path = asQualifierPartList()
        val firstPart = path.first()
        var currentDescriptor: DeclarationDescriptor? = scopeForFirstPart.findDescriptor(firstPart)
        currentDescriptor = currentDescriptor ?:
                moduleDescriptor.getPackage(FqName.topLevel(firstPart.name)).let { if (it.isEmpty()) null else it }

        if (currentDescriptor == null) return
        else storeSimpleNameExpression(firstPart.expression!!, currentDescriptor, trace)

        // TODO(lmr): we need to add visibility checks into this function...
        for (qualifierPartIndex in 1 until path.size) {
            val qualifierPart = path[qualifierPartIndex]

            val nextPackageOrClassDescriptor =
                when (currentDescriptor) {
                    // TODO(lmr): i wonder if we could allow this for Ktx. Seems like a nice to have
                    is TypeAliasDescriptor -> // TODO type aliases as qualifiers? (would break some assumptions in TypeResolver)
                        null
                    is ClassDescriptor -> {
                        var next: DeclarationDescriptor? = null
                        next = next ?: currentDescriptor.unsubstitutedInnerClassesScope.findDescriptor(qualifierPart)
                        if (currentDescriptor.kind == ClassKind.OBJECT) {
                            next = next ?: currentDescriptor.unsubstitutedMemberScope.findDescriptor(qualifierPart)
                        }
                        val cod = currentDescriptor.companionObjectDescriptor
                        if (cod != null) {
                            next = next ?: cod.unsubstitutedMemberScope.findDescriptor(qualifierPart)
                        }
                        next = next ?: currentDescriptor.staticScope.findDescriptor(qualifierPart)
                        next
                    }
                    is PackageViewDescriptor -> {
                        val packageView =
                            if (qualifierPart.typeArguments == null) {
                                moduleDescriptor.getPackage(currentDescriptor.fqName.child(qualifierPart.name))
                            } else null
                        if (packageView != null && !packageView.isEmpty()) {
                            packageView
                        } else {
                            currentDescriptor.memberScope.findDescriptor(qualifierPart)
                        }
                    }
                    is VariableDescriptor -> {
                        currentDescriptor.type.memberScope.findDescriptor(qualifierPart)
                    }
                    else -> null
                }

            if (nextPackageOrClassDescriptor == null) return
            else storeSimpleNameExpression(qualifierPart.expression!!, nextPackageOrClassDescriptor, trace)

            currentDescriptor = nextPackageOrClassDescriptor
        }
    }

    private fun storeSimpleNameExpression(
        expression: KtSimpleNameExpression,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace
    ) {
        trace.record(BindingContext.REFERENCE_TARGET, expression, descriptor)
        UnderscoreUsageChecker.checkSimpleNameUsage(descriptor, expression, trace)

        val qualifier = when (descriptor) {
            is PackageViewDescriptor -> PackageQualifier(expression, descriptor)
            is ClassDescriptor -> ClassQualifier(expression, descriptor)
            is TypeParameterDescriptor -> TypeParameterQualifier(expression, descriptor)
            is TypeAliasDescriptor -> descriptor.classDescriptor?.let {
                TypeAliasQualifier(expression, descriptor, it)
            }
            else -> null
        }

        if (qualifier != null) {
            trace.record(BindingContext.QUALIFIER, qualifier.expression, qualifier)
        }
    }
/*
    private fun resolveQualifiedName(
        expression: KtQualifiedExpression,
        context: ExpressionTypingContext
    ): KtxTagResolveInfo? {
        val currentContext = context
            .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
            .replaceContextDependency(ContextDependency.INDEPENDENT)

        expression.elementChain(currentContext)

        val selector = expression.selectorExpression
        val receiverExpr = expression.receiverExpression

        val receiverTypeInfo = when (context.trace.get(BindingContext.QUALIFIER, receiverExpr)) {
            null -> facade.getTypeInfo(receiverExpr, currentContext)
            else -> KotlinTypeInfo(null, currentContext.dataFlowInfo)
        }

        // TODO(lmr): inspect jumps and nullability. We cant allow tags that can be null or return early
        val receiverType = receiverTypeInfo.type
            ?: ErrorUtils.createErrorType("Type for " + receiverExpr.text)

        val receiver = context.trace.get(BindingContext.QUALIFIER, receiverExpr)
            ?: ExpressionReceiver.create(receiverExpr, receiverType, context.trace.bindingContext)

//        return when (selector) {
//            is KtSimpleNameExpression -> resolveFunction(receiver, receiverExpr, selector, context, null)
//            else -> null
//        }
        return null
    }
*/
    /*private fun resolveComposer(
        tagExpr: KtExpression,
        element: KtxElement,
        context: ExpressionTypingContext
    ): KtxComposerInfo? {
        val resolvedContextProperty = resolveVar(COMPOSER_NAME, tagExpr, context)
        if (!resolvedContextProperty.isSuccess) return null

        val descriptor = resolvedContextProperty.resultingCall.resultingDescriptor

        val type = when (descriptor) {
            is PropertyDescriptor -> descriptor.type
            is VariableDescriptor -> descriptor.type
            else -> return null
        }


//        val getter = (resolvedContextProperty.resultingCall.resultingDescriptor as? PropertyDescriptor)?.getter
//        if (getter == null) return null
//
//        val type = getter.returnType
//        if (type != null) {

        // TODO(b/???): This parameter types of the emits are not validated and will generate a runtime error if not correct.
        val emitResult = resolveMethod(type, EMIT_NAME, tagExpr, element, context)

        // Calculate emit descriptors
        val simpleEmitMap = mutableMapOf<KotlinType, ResolvedCall<*>>()
        val compoundEmitMap = mutableMapOf<KotlinType, ResolvedCall<*>>()
        if (emitResult.isSuccess || emitResult.isIncomplete || emitResult.isAmbiguity) {
            val emitCalls = emitResult.resultingCalls.filter {
                it.resultingDescriptor.valueParameters.let {
                    it.size >= 3 && it.size <= 4
                }
            }

            for (emitCall in emitCalls) {
                val ctor = emitCall.candidateDescriptor.valueParameters.find { it.name == CTOR_PARAMETER_NAME }
                if (ctor != null) {
                    val children = emitCall.candidateDescriptor.valueParameters.find { it.name == CHILDREN_PARAMETER_NAME }
                    if (ctor.type.isFunctionTypeOrSubtype) {
                        val ctorResult = ctor.type.getReturnTypeFromFunctionType()
                        val constraint: Collection<KotlinType> = if (ctorResult.isTypeParameter()) {
                            TypeUtils.getTypeParameterDescriptorOrNull(ctorResult)?.upperBounds ?: emptyList()
                        } else {
                            listOf(ctorResult)
                        }
                        val map = if (children != null) compoundEmitMap else simpleEmitMap
                        for (constraintType in constraint)
                            map[constraintType] = emitCall
                    }
                }
            }
        }

        // Calculate memoization calls
        var statelessMemoize: ResolvedCall<*>? = null
        var statefullMemoize: ResolvedCall<*>? = null
        val memoizeResult = resolveMethod(type, MEMOIZE_NAME, tagExpr, element, context)
        if (memoizeResult.isSuccess || memoizeResult.isIncomplete || memoizeResult.isAmbiguity) {
            val memoizeCalls = memoizeResult.resultingCalls.filter {
                it.resultingDescriptor.valueParameters.let {
                    it.size >= 3 && it.size <= 4
                }
            }

            for (memoizeCall in memoizeCalls) {
                if (memoizeCall.candidateDescriptor.valueParameters.find { it.name == CTOR_PARAMETER_NAME } != null) {
                    statefullMemoize = memoizeCall
                } else {
                    statelessMemoize = memoizeCall
                }

            }
        }


        // Calculate joinKey
        val joinKey: ResolvedCall<*>? = resolveMethod(type, JOINKEY_NAME, tagExpr, element, context).let {
            if (it.isSuccess || it.isIncomplete) it.resultingCall else null
        }

        return KtxComposerInfo(
            type = type,
            descriptor = descriptor,
            simpleEmitMap = simpleEmitMap,
            compoundEmitMap = compoundEmitMap,
            statelessMemoize = statelessMemoize ?: statefullMemoize,
            statefullMemoize = statefullMemoize,
            joinKey = joinKey
        )
    }*/
}

// We want to return null in cases where types mismatch, so we use this heuristic to find out. I think there might be a more robust
// way to find this out, but I'm not sure what it would be
private fun BindingTrace.hasTypeMismatchErrorsOn(element: KtElement): Boolean =
    bindingContext.diagnostics.forElement(element).filter { it.severity == Severity.ERROR }.isNotEmpty()

private fun KtExpression.asQualifierPartList(): List<QualifiedExpressionResolver.QualifierPart> {
    val result = SmartList<QualifiedExpressionResolver.QualifierPart>()

    fun addQualifierPart(expression: KtExpression?): Boolean {
        if (expression is KtSimpleNameExpression) {
            result.add(QualifiedExpressionResolver.ExpressionQualifierPart(expression.getReferencedNameAsName(), expression))
            return true
        }
        return false
    }

    var expression: KtExpression? = this
    while (true) {
        if (addQualifierPart(expression)) break
        if (expression !is KtQualifiedExpression) break

        addQualifierPart(expression.selectorExpression)

        expression = expression.receiverExpression
    }

    return result.asReversed()
}

private fun HierarchicalScope.findDescriptor(part: QualifiedExpressionResolver.QualifierPart): DeclarationDescriptor? {
    return findFirstFromMeAndParent {
        it.findVariable(part.name, part.location)
            ?: it.findFunction(part.name, part.location)
            ?: it.findClassifier(part.name, part.location)
    }
}

private fun MemberScope.findDescriptor(part: QualifiedExpressionResolver.QualifierPart): DeclarationDescriptor? {
    return this.getContributedClassifier(part.name, part.location)
        ?: getContributedFunctions(part.name, part.location).singleOrNull()
        ?: getContributedVariables(part.name, part.location).singleOrNull()
}