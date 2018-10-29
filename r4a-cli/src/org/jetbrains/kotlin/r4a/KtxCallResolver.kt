package org.jetbrains.kotlin.r4a

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
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
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyForInvoke
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategyImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

class KtxCallResolver(
    private val callResolver: CallResolver,
    private val facade: ExpressionTypingFacade,
    private val project: Project
) {
    private val psiFactory = KtPsiFactory(project, markGenerated = false)

    private lateinit var composerType: KotlinType
    private lateinit var composerResolvedCall: ResolvedCall<*>
    private lateinit var tagExpressions: List<KtExpression>
    private lateinit var joinKeyCall: ResolvedCall<*>

    private val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()
    private val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()
    private val emittableTypeToImplicitCtorTypes = mutableListOf<Pair<List<KotlinType>, Set<KotlinType>>>()

    private fun isStatic(
        expression: KtExpression,
        context: ExpressionTypingContext,
        expectedType: KotlinType?,
        constantChecker: ConstantExpressionEvaluator
    ): Boolean {
        val constValue = constantChecker.evaluateExpression(expression, context.trace, expectedType)
        return constValue != null
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

        val calleeExpression = psiFactory.createSimpleName(EMIT_NAME.asString())

        val emitCall = makeCall(
            callElement = element,
            calleeExpression = calleeExpression,
            receiver = TransientReceiver(composerType)
        )

        val contextForVariable = BasicCallResolutionContext.create(
            context,
            emitCall,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, emitCall)
        ).replaceCollectAllCandidates(true)

        val results = callResolver.resolveCallWithGivenName(
            contextForVariable,
            emitCall,
            calleeExpression,
            EMIT_NAME
        )

        for (candidate in results.allCandidates ?: emptyList()) {
            recordComposerEmitBounds(candidate.candidateDescriptor)
        }

        joinKeyCall = resolveJoinKey(
            expressionToReportErrorsOn = tagExpressions.first(),
            context = context
        ) ?: return false

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
        with(descriptor) {
            // TODO(lmr): we could report diagnostics on some of these? seems strange to though...
            if (valueParameters.size < 3) return
            if (valueParameters.size > 4) return
            val ctorParam = valueParameters.find { it.name == CTOR_PARAMETER_NAME } ?: return
            if (!ctorParam.type.isFunctionTypeOrSubtype) return

            val upperBounds = ctorParam.type.getReturnTypeFromFunctionType().upperBounds()
            val implicitParamTypes = ctorParam.type.getValueParameterTypesFromFunctionType().map { it.type }

            for (implicitType in implicitParamTypes) {
                emittableTypeToImplicitCtorTypes.add(
                    upperBounds to implicitParamTypes.toSet()
                )
            }

            emitSimpleUpperBoundTypes.addAll(upperBounds)

            if (valueParameters.any { it.name == CHILDREN_PARAMETER_NAME }) {
                emitCompoundUpperBoundTypes.addAll(upperBounds)
            }
        }
    }

    private class ImplicitCtorValueArgument(val type: KotlinType) : ValueArgument {
        override fun getArgumentExpression(): KtExpression? = null
        override fun getArgumentName(): ValueArgumentName? = null
        override fun isNamed(): Boolean = false
        override fun asElement(): KtElement = error("tried to get element")
        override fun getSpreadElement(): LeafPsiElement? = null
        override fun isExternal(): Boolean = true
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

            val constantChecker = ConstantExpressionEvaluator(
                project = project,
                module = context.scope.ownerDescriptor.module,
                languageVersionSettings = context.languageVersionSettings
            )

            val usedAttributeNodes = emitOrCall
                .allAttributes()
                .map { it.name to it }
                .toMap()
                .values
                .map {
                    val attr = attrInfos[it.name] ?: error("couldnt find attribute ${it.name}")
                    AttributeNode(
                        name = it.name,
                        descriptor = it.descriptor,
                        expression = attr.value,
                        type = it.type,
                        isStatic = isStatic(attr.value, context, it.type, constantChecker)
                    )
                }

            return ResolvedKtxElementCall(
                usedAttributes = usedAttributeNodes,
                unusedAttributes = (attrInfos.keys - usedAttributes).toList(),
                emitOrCall = emitOrCall,
                getComposerCall = composerResolvedCall
            )
        }

        return null
    }

    private fun forceResolveCallForInvoke(
        calleeType: KotlinType,
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<FunctionDescriptor> {
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

            val usedAttributeInfos = mutableListOf<TempParameterInfo>()

            val resolvedCall = resolveCandidate(
                kind,
                result,
                call,
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                candidateContext
            )

            if (resolvedCall == null) return@mapNotNull null

            val returnType = resolvedCall.resultingDescriptor.returnType


            if (returnType == null || returnType.isUnit()) {
                // bottomed out

                // TODO(lmr): check that the function was @Composable

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

            if (returnType.isEmittable()) {

                val composerCall = resolveComposerEmit(
                    constructedType = returnType,
                    hasBody = attributes.contains(CHILDREN_KEY),
                    implicitCtorTypes = resolvedCall.call.valueArguments.mapNotNull { (it as? ImplicitCtorValueArgument)?.type },
                    expressionToReportErrorsOn = expression,
                    context = context
                )

                val updateReceiverScope = composerCall?.let {
                    it.resultingDescriptor.valueParameters.getOrNull(2)?.type?.getReceiverTypeFromFunctionType()
                }

                val setterValidations = resolveAllSetAttributes(
                    expression,
                    updateReceiverScope,
                    returnType,
                    attributes,
                    attrsUsedInCall,
                    attrsUsedInSets,
                    candidateContext
                )

                val pivotals = resolvePivotalAttributes(
                    attributes,
                    attrsUsedInCall,
                    usedAttributeInfos,
                    setterValidations,
                    returnType
                )


                return@mapNotNull TempResolveInfo(
                    true,
                    tmpForCandidate,
                    attributes.size - attrsUsedInCall.size
                ) {
                    EmitCallNode(
                        memoize = ComposerCallInfo(
                            composerCall = composerCall,
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = resolvedCall,
                            ctorParams = constructCtorValueNodes(resolvedCall, attributes),
                            validations = setterValidations
                        )
                    )
                }
            }

            val composerCall = resolveComposerCall(
                constructedType = returnType,
                expressionToReportErrorsOn = expression,
                context = context
            )

            // the "invalid" lambda is at a different argument index depending on whether or not there is a "ctor" param.
            val invalidArgIndex = if (returnType != null) 2 else 1

            val invalidReceiverScope = composerCall?.let {
                it.resultingDescriptor.valueParameters.getOrNull(invalidArgIndex)?.type?.getReceiverTypeFromFunctionType()
            }

            val setterValidations = resolveAllSetAttributes(
                expression,
                invalidReceiverScope,
                returnType,
                attributes,
                attrsUsedInCall,
                attrsUsedInSets,
                candidateContext
            )

            val pivotals = resolvePivotalAttributes(
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                setterValidations,
                returnType
            )

            val attrsUsedInFollowingCalls = mutableSetOf<String>()

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
                        composerCall = composerCall,
                        pivotals = pivotals,
                        joinKeyCall = joinKeyCall,
                        ctorCall = resolvedCall,
                        ctorParams = constructAttributeNodes(resolvedCall, attributes),
                        validations = setterValidations + childCall.consumedAttributes().toSet().map {
                            val attr = attributes[it.name] ?: error("didnt find attribute")
                            it.asChangedValidatedAssignment(
                                expressionToReportErrorsOn = attr.key ?: expression,
                                receiverScope = invalidReceiverScope,
                                valueExpr = attr.value,
                                context = context
                            )
                        }
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
        attributes: Map<String, AttributeInfo>,
        attrsUsedInCall: Set<String>,
        callParamInfos: List<TempParameterInfo>,
        validations: List<ValidatedAssignment>,
        returnType: KotlinType?
    ): List<AttributeNode> {
        val result = mutableListOf<AttributeNode>()

        if (returnType == null || returnType.isUnit()) {
            return callParamInfos
                .filter { isAnnotatedAsPivotal(it.descriptor) }
                .map {
                    AttributeNode(
                        name = it.attribute.name,
                        descriptor = it.descriptor,
                        type = it.type,
                        expression = it.attribute.value,
                        isStatic = false
                    )
                }
        }

        // if you were in the ctor call but not in the sets, you *have* to be pivotal
        for (info in callParamInfos) {
            if (attrsUsedInCall.contains(info.attribute.name)) continue
            val attribute = attributes[info.attribute.name] ?: continue
            result.add(
                AttributeNode(
                    name = info.attribute.name,
                    descriptor = info.descriptor,
                    type = info.type,
                    expression = attribute.value,
                    isStatic = false
                )
            )
        }

        // There are additional cases where attributes can be pivotal:
        //   1. It is annotated as @Pivotal
        //   2. It is a `val` ctor parameter
        for (assignment in validations) {
            val attribute = assignment.attribute
            val name = attribute.name
            val descriptor = attribute.descriptor

            if (isAnnotatedAsPivotal(descriptor)) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
            if (descriptor is PropertyDescriptor && attrsUsedInCall.contains(name) && !descriptor.isVar) {
                result.add(
                    AttributeNode(
                        name = name,
                        descriptor = descriptor,
                        type = attribute.type,
                        expression = attribute.expression,
                        isStatic = false
                    )
                )
                continue
            }
        }

        return result
    }

    private fun constructAttributeNodes(resolvedCall: ResolvedCall<*>, attributes: Map<String, AttributeInfo>): List<AttributeNode> {
        return resolvedCall.resultingDescriptor.valueParameters.mapNotNull { param ->
            val name = param.name.asString()
            var attr = attributes[name]

            if (isAnnotatedAsChildren(param)) {
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

            if (isAnnotatedAsChildren(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    attr = childrenAttr
                }
            }

            if (attr == null && isImplicitConstructorParam(param, resolvedCall.resultingDescriptor)) {
                return@mapNotNull ImplicitCtorValueNode(
                    name = param.name.asString(),
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

    private fun AttributeNode.asChangedValidatedAssignment(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType?,
        valueExpr: KtExpression,
        context: ExpressionTypingContext
    ): ValidatedAssignment {
        val validationCall = resolveValidationCall(
            validationType = ValidationType.CHANGED,
            attrType = type,
            expressionToReportErrorsOn = expressionToReportErrorsOn,
            receiverScope = receiverScope,
            valueExpr = valueExpr,
            context = context
        )

        return ValidatedAssignment(
            validationType = ValidationType.CHANGED,
            validationCall = validationCall,
            attribute = this,
            assignment = null
        )
    }

    private fun resolveAllSetAttributes(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType?,
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

            // TODO(lmr): it might be possible to construct a case where private var properties are resolved here
            // even though they shouldn't be props at that point... perhaps we should look into that corner case

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

                val validationType = when {
                    attributesUsedInCall.contains(name) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val validationCall = resolveValidationCall(
                    expressionToReportErrorsOn,
                    receiverScope,
                    validationType,
                    resolvedCall.resultingDescriptor.valueParameters.first().type,
                    attribute.value,
                    context
                )

                results.add(
                    ValidatedAssignment(
                        validationType = validationType,
                        assignment = resolvedCall,
                        attribute = AttributeNode(
                            name = name,
                            expression = attribute.value,
                            type = resolvedCall.resultingDescriptor.valueParameters.first().type,
                            descriptor = resolvedCall.resultingDescriptor,
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
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

                val validationType = when {
                    attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val attrType = resolvedCall.resultingDescriptor.valueParameters.first().type

                val validationCall = resolveValidationCall(
                    expressionToReportErrorsOn,
                    receiverScope,
                    validationType,
                    attrType,
                    children.value,
                    context
                )

                results.add(
                    ValidatedAssignment(
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
                        validationCall = validationCall
                    )
                )
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
    ): ResolvedCall<*>? {
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
    ): ResolvedCall<*>? {
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

    private class TempParameterInfo(
        val attribute: AttributeInfo,
        val descriptor: DeclarationDescriptor,
        val type: KotlinType
    )

    private fun resolveCandidate(
        kind: ResolveKind,
        candidate: ResolvedCall<*>,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        usedAttributeInfos: MutableList<TempParameterInfo>,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val valueArguments = mutableListOf<ValueArgument>()

        val referencedDescriptor = candidate.resultingDescriptor

        for (param in referencedDescriptor.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && isAnnotatedAsChildren(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)
                    arg = childrenAttr.makeArgumentValue(name)
                }
            }

            if (arg == null && attr != null) {
                usedAttributes.add(name)
                usedAttributeInfos.add(
                    TempParameterInfo(
                        attribute = attr,
                        descriptor = param,
                        type = param.type
                    )
                )
                context.trace.record(BindingContext.REFERENCE_TARGET, attr.key, param)
                arg = attr.makeArgumentValue()
            }

            if (arg == null && isImplicitConstructorParam(param, referencedDescriptor)) {
                arg = ImplicitCtorValueArgument(param.type)
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
        private fun isAnnotatedAsChildren(it: Annotated): Boolean {
            return it.annotations.findAnnotation(CHILDREN_FQNAME) != null
        }

        private fun isAnnotatedAsPivotal(it: Annotated): Boolean {
            return it.annotations.findAnnotation(PIVOTAL_FQNAME) != null
        }

        private fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(CHILDREN_FQNAME) != null

        private val CHILDREN_KEY = "<children>"

        private val COMPOSABLE_FQNAME = R4aUtils.r4aFqName("Composable")
        private val PIVOTAL_FQNAME = R4aUtils.r4aFqName("Pivotal")
        private val CHILDREN_FQNAME = R4aUtils.r4aFqName("Children")
        private val EMITTABLE_FQNAME = R4aUtils.r4aFqName("Emittable")

        private val COMPOSER_NAME = Name.identifier("composer")
        private val EMIT_NAME = Name.identifier("emit")
        private val CALL_NAME = Name.identifier("call")
        private val JOINKEY_NAME = Name.identifier("joinKey")
        private val CTOR_PARAMETER_NAME = Name.identifier("ctor")
        private val CHILDREN_PARAMETER_NAME = Name.identifier("children")
    }

    private fun isImplicitConstructorParam(
        param: ValueParameterDescriptor,
        fnDescriptor: CallableDescriptor
    ): Boolean {
        val returnType = fnDescriptor.returnType ?: return false
        val paramType = param.type
        for ((upperBounds, implicitTypes) in emittableTypeToImplicitCtorTypes) {
            if (!implicitTypes.any { it.isSubtypeOf(paramType) }) continue
            if (!returnType.satisfiesConstraintsOf(upperBounds)) continue
            return true
        }
        return false
    }

    private var uniqueName: () -> String = ({
        var i = 100000
        { "tmpVar${i++}" }
    }())

    private fun makeValueArgument(type: KotlinType, context: ExpressionTypingContext): ValueArgument {
        val fakeExpr = psiFactory.createSimpleName(uniqueName())

        context.trace.record(
            BindingContext.EXPRESSION_TYPE_INFO, fakeExpr, KotlinTypeInfo(
                type = type,
                dataFlowInfo = DataFlowInfo.EMPTY,
                jumpOutPossible = false,
                jumpFlowInfo = DataFlowInfo.EMPTY
            )
        )

        return CallMaker.makeValueArgument(fakeExpr)
    }

    private fun resolveJoinKey(
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        return resolveSubstitutableComposerMethod(
            JOINKEY_NAME,
            listOf(
                builtIns.anyType,
                builtIns.anyType
            ),
            null,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveComposerEmit(
        implicitCtorTypes: List<KotlinType>,
        constructedType: KotlinType,
        hasBody: Boolean,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        return resolveSubstitutableComposerMethod(
            EMIT_NAME,
            listOfNotNull(
                builtIns.anyType,
                functionType(
                    parameterTypes = implicitCtorTypes,
                    returnType = constructedType
                ),
                functionType(),
                if (hasBody) functionType() else null
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private val builtIns = DefaultBuiltIns.Instance

    private fun functionType(
        parameterTypes: List<KotlinType> = emptyList(),
        returnType: KotlinType = builtIns.unitType,
        receiverType: KotlinType? = null
    ): KotlinType = createFunctionType(
        builtIns = builtIns,
        annotations = Annotations.EMPTY,
        parameterNames = null,
        parameterTypes = parameterTypes,
        receiverType = receiverType,
        returnType = returnType
    )

    private fun resolveComposerCall(
        constructedType: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        // call signature is:
        // ==================
        // key: Any, invalid: V.() -> Boolean, block: () -> Unit
        // key: Any, ctor: () -> T, invalid: V.(T) -> Boolean, block: (T) -> Unit

        return resolveSubstitutableComposerMethod(
            CALL_NAME,
            listOfNotNull(
                builtIns.anyType,
                constructedType?.let {
                    functionType(returnType = constructedType)
                },
                functionType(
                    parameterTypes = listOfNotNull(constructedType),
                    returnType = builtIns.booleanType
                ),
                functionType(parameterTypes = listOfNotNull(constructedType))
            ),
            constructedType,
            expressionToReportErrorsOn,
            context
        )
    }

    private fun resolveValidationCall(
        expressionToReportErrorsOn: KtExpression,
        receiverScope: KotlinType?,
        validationType: ValidationType,
        attrType: KotlinType,
        valueExpr: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {

        if (receiverScope == null) return null

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val name = validationType.name.toLowerCase()
        val includeLambda = validationType != ValidationType.CHANGED

        val calleeExpression = psiFactory.createSimpleName(name)

        val call = makeCall(
            callElement = expressionToReportErrorsOn,
            calleeExpression = calleeExpression,
            valueArguments = listOfNotNull(
                CallMaker.makeValueArgument(valueExpr),
                if (includeLambda) makeValueArgument(functionType(parameterTypes = listOf(attrType)), contextToUse)
                else null
            ),
            receiver = TransientReceiver(receiverScope)
        )

        // ValidatorType.set(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.update(AttrType, (AttrType) -> Unit): Boolean
        // ValidatorType.changed(AttrType): Boolean

        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            calleeExpression,
            Name.identifier(name)
        )

        if (results.isSuccess) return results.resultingCall

        return null
    }

    private fun resolveSubstitutableComposerMethod(
        methodName: Name,
        argumentTypes: List<KotlinType>,
        typeToSubstitute: KotlinType?,
        expressionToReportErrorsOn: KtExpression,
        context: ExpressionTypingContext
    ): ResolvedCall<*>? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve variable", expressionToReportErrorsOn
        )
        val contextToUse = context.replaceTraceAndCache(temporaryForVariable)

        val composerExpr = psiFactory.createSimpleName(methodName.asString())

        val call = makeCall(
            callElement = expressionToReportErrorsOn,
            calleeExpression = composerExpr,
            receiver = TransientReceiver(composerType),
            valueArguments = argumentTypes.map { makeValueArgument(it, contextToUse) }
        )

        val results = callResolver.resolveCallWithGivenName(
            BasicCallResolutionContext.create(
                contextToUse,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
            ),
            call,
            composerExpr,
            methodName
        )

        if (results.isSuccess) return results.resultingCall

        if (typeToSubstitute == null) return null

        val candidates = if (context.collectAllCandidates) results.allCandidates ?: emptyList() else results.resultingCalls

        for (candidate in candidates) {

            val T = candidate.typeArguments.keys.singleOrNull() ?: continue

            if (!typeToSubstitute.satisfiesConstraintsOf(T)) continue

            val nextTempTrace = TemporaryTraceAndCache.create(
                context, "trace to resolve variable", expressionToReportErrorsOn
            )

            val nextContext = context
                .replaceTraceAndCache(nextTempTrace)
                .replaceCollectAllCandidates(false)

            val substitutor = TypeSubstitutor.create(
                mapOf(
                    T.typeConstructor to typeToSubstitute.asTypeProjection()
                )
            )

            val nextCall = makeCall(
                callElement = expressionToReportErrorsOn,
                calleeExpression = composerExpr,
                receiver = TransientReceiver(composerType),
                valueArguments = candidate.candidateDescriptor.valueParameters.map { makeValueArgument(it.type, nextContext) }
            )

            val nextResults = callResolver.resolveCallWithKnownCandidate(
                nextCall,
                TracingStrategyImpl.create(composerExpr, nextCall),
                BasicCallResolutionContext.create(
                    nextContext,
                    nextCall,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
                ),
                ResolutionCandidate.create(
                    nextCall,
                    candidate.candidateDescriptor,
                    candidate.dispatchReceiver,
                    candidate.explicitReceiverKind,
                    substitutor
                ),
                DataFlowInfoForArgumentsImpl(nextContext.dataFlowInfo, nextCall)
            )

            if (nextResults.isSuccess) {
                nextTempTrace.commit()
                return nextResults.resultingCall
            }
        }

        return if (context.collectAllCandidates) null
        else resolveSubstitutableComposerMethod(
            methodName,
            argumentTypes,
            typeToSubstitute,
            expressionToReportErrorsOn,
            context.replaceCollectAllCandidates(true)
        )
    }

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
}

private fun KotlinType.satisfiesConstraintsOf(T: TypeParameterDescriptor): Boolean {
    return T.upperBounds.all { isSubtypeOf(it) }
}

private fun KotlinType.satisfiesConstraintsOf(bounds: List<KotlinType>): Boolean {
    return bounds.all { isSubtypeOf(it) }
}

// We want to return null in cases where types mismatch, so we use this heuristic to find out. I think there might be a more robust
// way to find this out, but I'm not sure what it would be
private fun BindingTrace.hasTypeMismatchErrorsOn(element: KtElement): Boolean =
    bindingContext.diagnostics.forElement(element).any { it.severity == Severity.ERROR }

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