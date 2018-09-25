package org.jetbrains.kotlin.r4a

import com.intellij.lang.ASTNode
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.analysis.ComposableType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.noErrorsInValueArguments
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
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
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.isUnit


class KtxTagInfo(
    val composableType: ComposableType,
    val isConstructed: Boolean,
    val resolvedCall: ResolvedCall<*>,
    val instanceType: KotlinType?,
    val attributeInfos: List<KtxAttributeInfo>,
    val childrenInfo: KtxAttributeInfo?,
    val parameterInfos: List<KtxTagParameterInfo>,
    val referrableDescriptor: DeclarationDescriptor
)

class KtxTagResolveInfo(
    val valid: Boolean,
    val isConstructed: Boolean,
    val resolvedCall: ResolvedCall<*>,
    val typeDescriptor: ClassDescriptor?,
    val referrableDescriptor: DeclarationDescriptor,
    val instanceType: KotlinType?,
    val parameters: List<KtxTagParameterInfo>,
    val receiverExpression: KtExpression?,
    private val trace: TemporaryTraceAndCache
) {
    fun commit() { trace.commit() }
}

class KtxTagParameterInfo(
    val name: String,
    val isContext: Boolean,
    val isChildren: Boolean,
    val descriptor: ValueParameterDescriptor
)

class KtxAttributeInfo(
    val name: String,
    val type: KotlinType,
    val isPivotal: Boolean,
    val isIncludedInConstruction: Boolean,
    val setterResolvedCall: ResolvedCall<*>?,
    val descriptor: DeclarationDescriptor
)

class KtxTagResolver(
    val callResolver: CallResolver,
    val facade: ExpressionTypingFacade,
    val attributeExpressions: Map<String, KtExpression>,
    val bodyLambdaExpression: KtLambdaExpression?
) {
    var attributesInConstruction: Set<String> = emptySet()

    companion object {
        val CONTEXT_ARGUMENT = object : ValueArgument {
            override fun getArgumentExpression(): KtExpression? = null
            override fun getArgumentName(): ValueArgumentName? = null
            override fun isNamed(): Boolean = false
            override fun asElement(): KtElement = error("tried to get element")
            override fun getSpreadElement(): LeafPsiElement? = null
            override fun isExternal(): Boolean = true
        }

        fun isContextParameter(it: ValueParameterDescriptor): Boolean {
            // TODO(lmr): need better approach for this
            return it.type.constructor.declarationDescriptor?.fqNameSafe == FqName("android.content.Context")
        }

        private val childrenFqName = R4aUtils.r4aFqName("Children")

        fun isChildrenParameter(it: ValueParameterDescriptor): Boolean {
            return it.annotations.findAnnotation(childrenFqName) != null
        }
    }

    private fun makeCall(
        tagExpr: KtExpression,
        valueArguments: List<ValueArgument>,
        receiver: Receiver?
    ): Call {
        return object : Call {
            override fun getDispatchReceiver(): ReceiverValue? = null
            override fun getValueArgumentList(): KtValueArgumentList? = null
            override fun getTypeArgumentList(): KtTypeArgumentList? = null
            override fun getExplicitReceiver(): Receiver? = receiver
            override fun getCalleeExpression(): KtExpression? = tagExpr
            override fun getValueArguments(): List<ValueArgument> = valueArguments
            override fun getCallElement(): KtElement = tagExpr
            override fun getFunctionLiteralArguments(): List<LambdaArgument> = listOf()
            override fun getTypeArguments(): List<KtTypeProjection> = listOf()
            override fun getCallType(): Call.CallType = Call.CallType.DEFAULT
            override fun getCallOperationNode(): ASTNode? = null
        }
    }

    private fun makeCallWithValueArguments(
        setterCall: Call,
        setterCallValueArguments: List<ValueArgument>
    ): Call {
        return object : Call {
            override fun getCallOperationNode() = setterCall.callOperationNode
            override fun getExplicitReceiver() = setterCall.explicitReceiver
            override fun getDispatchReceiver() = setterCall.dispatchReceiver
            override fun getCalleeExpression() = setterCall.calleeExpression
            override fun getValueArgumentList() = setterCall.valueArgumentList
            override fun getValueArguments() = setterCallValueArguments
            override fun getFunctionLiteralArguments() = setterCall.functionLiteralArguments
            override fun getTypeArguments() = setterCall.typeArguments
            override fun getTypeArgumentList() = setterCall.typeArgumentList
            override fun getCallElement() = setterCall.callElement
            override fun getCallType() = setterCall.callType
        }
    }

    private fun resolveFunction(
        receiver: Receiver?,
        receiverExpression: KtExpression?,
        expression: KtSimpleNameExpression,
        context: ExpressionTypingContext,
        parameterInfos: List<KtxTagParameterInfo>?
    ): KtxTagResolveInfo? {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", expression
        )

        val valueArguments = (parameterInfos ?: emptyList()).mapNotNull {
            val expr = attributeExpressions[it.name]
            when {
                it.isChildren -> {
                    (bodyLambdaExpression ?: expr)?.let { CallMaker.makeExternalValueArgument(it) }
                }
                expr != null -> CallMaker.makeExternalValueArgument(expr)
                it.isContext -> CONTEXT_ARGUMENT
                else -> attributeExpressions[it.name]?.let { CallMaker.makeExternalValueArgument(it) }
            }
        }

        val call = makeCall(expression, valueArguments, receiver)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        val result = callResolver.resolveFunctionCall(contextForVariable)

        if (result.isNothing) return null

        if (!result.isSuccess && parameterInfos == null && result.resultingCalls.isNotEmpty()) {
            val allResults = mutableListOf<KtxTagResolveInfo>()
            for (candidate in result.resultingCalls) {
                val nextParameters = candidate.resultingDescriptor.valueParameters
                    .map {
                        KtxTagParameterInfo(
                            name = it.name.asString(),
                            descriptor = it,
                            isChildren = isChildrenParameter(it),
                            isContext = isContextParameter(it)
                        )
                    }
                val nextResult = resolveFunction(
                    receiver,
                    receiverExpression,
                    expression,
                    context,
                    nextParameters
                ) ?: continue

                if (nextResult.valid) {
                    return nextResult
                }

                allResults.add(nextResult)
            }

            return allResults.maxBy { it.parameters.size }
        }

        val resolvedCall = result.resultingCall
        val referencedDescriptor = when (resolvedCall) {
            is VariableAsFunctionResolvedCall -> resolvedCall.variableCall.resultingDescriptor
            else -> resolvedCall.resultingDescriptor
        }

        storeSimpleNameExpression(expression, referencedDescriptor, context.trace)

        val resultingDescriptor = resolvedCall.resultingDescriptor

        val typeDescriptor = when (resultingDescriptor) {
            is ConstructorDescriptor -> resultingDescriptor.containingDeclaration as? ClassDescriptor
            is ClassDescriptor -> resultingDescriptor
            else -> null
        }

        // TODO(lmr): theoretically we could allow for "factory functions" to be used which had return types.
        // Pushing this decision off for now.
        val isConstructed = resultingDescriptor is ConstructorDescriptor

        val referrableDescriptor: DeclarationDescriptor = when {
            resolvedCall is VariableAsFunctionResolvedCall -> resolvedCall.variableCall.candidateDescriptor
            resultingDescriptor is ConstructorDescriptor -> resultingDescriptor.constructedClass
            else -> resolvedCall.resultingDescriptor
        }

        val instanceType = when (resultingDescriptor) {
            is ConstructorDescriptor -> resultingDescriptor.returnType
            else -> null
        }

        return KtxTagResolveInfo(
            valid = result.isSuccess,
            isConstructed = isConstructed,
            resolvedCall = resolvedCall,
            receiverExpression = receiverExpression,
            referrableDescriptor = referrableDescriptor,
            typeDescriptor = typeDescriptor,
            instanceType = instanceType,
            trace = temporaryForVariable,
            parameters = resolvedCall.valueArguments.map {
                KtxTagParameterInfo(
                    name = it.key.name.asString(),
                    descriptor = it.key,
                    isContext = isContextParameter(it.key),
                    isChildren = isChildrenParameter(it.key)
                )
            }
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

        return when (selector) {
            is KtSimpleNameExpression -> resolveFunction(receiver, receiverExpr, selector, context, null)
            else -> null
        }
    }

    fun resolveReference(
        expression: KtExpression,
        context: ExpressionTypingContext
    ): KtxTagResolveInfo? {
        return when (expression) {
            is KtSimpleNameExpression -> resolveFunction(null, null, expression, context, null)
            is KtQualifiedExpression -> resolveQualifiedName(expression, context)
            else -> null
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

    fun resolveAttributeAsSetter(
        name: String,
        keyExpr: KtReferenceExpression,
        valueExpr: KtExpression,
        tag: KtxTagResolveInfo,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): KtxAttributeInfo? {
        val instanceType = tag.instanceType ?: return null

        if (instanceType.isUnit()) {
            // NOTE(lmr): this should only really happen on function components and function instances
            // in which case we have already handled all of the attributes in the resolving of the tag
            return null
        }

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

        val receiver = ExpressionReceiver.create(
            valueExpr,
            instanceType,
            context.trace.bindingContext
        )

        val call = makeCall(
            keyExpr,
            listOf(CallMaker.makeValueArgument(valueExpr)),
            receiver
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

        return KtxAttributeInfo(
            name = name,
            type = resolvedCall.resultingDescriptor.valueParameters[0].type,
            descriptor = resolvedCall.resultingDescriptor,
            setterResolvedCall = resolvedCall,
            isIncludedInConstruction = attributesInConstruction.contains(name),
            isPivotal = false
        )
    }

    fun resolveAttributeAsProperty(
        nameAsString: String,
        tagExpr: KtExpression,
        keyExpr: KtSimpleNameExpression,
        valueExpr: KtExpression,
        tag: KtxTagResolveInfo,
        expectedTypes: MutableCollection<KotlinType>,
        context: ExpressionTypingContext
    ): KtxAttributeInfo? {
        val instanceType = tag.instanceType ?: return null

        if (instanceType.isUnit()) {
            // NOTE(lmr): this should only really happen on function components and function instances
            // in which case we have already handled all of the
            return null
        }

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
        val receiver = ExpressionReceiver.create(
            tagExpr,
            instanceType,
            context.trace.bindingContext
        )

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
        val setterCall = makeCallWithValueArguments(
            resolvedCall.call,
            listOf(CallMaker.makeValueArgument(valueExpr))
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

        return KtxAttributeInfo(
            name = nameAsString,
            type = descriptor.type,
            descriptor = descriptor,
            setterResolvedCall = resolvedSetterCall,
            isIncludedInConstruction = attributesInConstruction.contains(nameAsString),
            isPivotal = !descriptor.isVar // NOTE(lmr): i don't think this can happen... it wouldn't resolve here in this case
        )
    }

    fun resolveChildrenAsSetter(
        childrenDescriptor: SimpleFunctionDescriptor,
        childrenExpr: KtExpression,
        tag: KtxTagResolveInfo,
        element: KtxElement,
        context: ExpressionTypingContext
    ): KtxAttributeInfo? {
        val instanceType = tag.instanceType ?: return null

        val refExpr = element.bodyLambdaExpression as KtxLambdaExpression


        if (instanceType.isUnit()) {
            // NOTE(lmr): this should only really happen on function components and function instances
            // in which case we have already handled all of the attributes in the resolving of the tag
            return null
        }

        val setterName = childrenDescriptor.name
        val name = R4aUtils.propertyNameFromSetterMethod(setterName.asString())

        val valueArguments = listOf(CallMaker.makeValueArgument(childrenExpr))
        val receiver = ExpressionReceiver.create(
            childrenExpr,
            instanceType,
            context.trace.bindingContext
        )
        val call = object : Call {
            override fun getDispatchReceiver(): ReceiverValue? = null
            override fun getValueArgumentList(): KtValueArgumentList? = null
            override fun getTypeArgumentList(): KtTypeArgumentList? = null
            override fun getCallOperationNode(): ASTNode? = null
            override fun getTypeArguments(): List<KtTypeProjection> = listOf()
            override fun getFunctionLiteralArguments(): List<LambdaArgument> = listOf()
            override fun getCallType(): Call.CallType = Call.CallType.DEFAULT

            override fun getExplicitReceiver(): Receiver? = receiver
            override fun getValueArguments(): List<ValueArgument> = valueArguments
            override fun getCallElement(): KtElement = element
            override fun getCalleeExpression(): KtExpression? = refExpr
        }

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
            refExpr,
            NewResolutionOldInference.ResolutionKind.Function
        )

        if (results.isNothing) {
            return null
        }

        if (temporaryForFunction.trace.hasTypeMismatchErrorsOn(childrenExpr)) {
            return null
        }

        val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(results, context) ?: return null

        temporaryForFunction.commit()

        return KtxAttributeInfo(
            name = name,
            type = resolvedCall.resultingDescriptor.valueParameters[0].type,
            descriptor = resolvedCall.resultingDescriptor,
            setterResolvedCall = resolvedCall,
            isIncludedInConstruction = attributesInConstruction.contains(name),
            isPivotal = false
        )
    }

    fun resolveChildrenAsProperty(
        propertyDescriptor: PropertyDescriptor,
        valueExpr: KtLambdaExpression,
        tag: KtxTagResolveInfo,
        element: KtxElement,
        context: ExpressionTypingContext
    ): KtxAttributeInfo? {
        val instanceType = tag.instanceType ?: return null
        val refExpr = element.bodyLambdaExpression as KtxLambdaExpression

        if (instanceType.isUnit()) {
            // NOTE(lmr): this should only really happen on function components and function instances
            // in which case we have already handled all of the
            return null
        }

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", element
        )

        val receiver = ExpressionReceiver.create(
            valueExpr,
            instanceType,
            context.trace.bindingContext
        )
        val call = object : Call {
            override fun getCallOperationNode(): ASTNode? = null
            override fun getValueArgumentList(): KtValueArgumentList? = null
            override fun getFunctionLiteralArguments(): List<LambdaArgument> = emptyList()
            override fun getTypeArguments(): List<KtTypeProjection> = emptyList()
            override fun getTypeArgumentList(): KtTypeArgumentList? = null
            override fun getDispatchReceiver(): ReceiverValue? = null
            override fun getCallType(): Call.CallType = Call.CallType.DEFAULT

            override fun getExplicitReceiver(): Receiver? = receiver
            override fun getValueArguments(): List<ValueArgument> = emptyList()
            override fun getCallElement(): KtElement = element
            override fun getCalleeExpression(): KtExpression? = refExpr
        }

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
            valueExpr,
            context
                .replaceTraceAndCache(temporaryForVariable)
                .replaceExpectedType((resolvedCall.resultingDescriptor).type)
                .replaceCallPosition(CallPosition.PropertyAssignment(null))
        )
        if (temporaryForVariable.trace.hasTypeMismatchErrorsOn(valueExpr)) {
            return null
        }
        val descriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        val setter = descriptor.setter ?: return null

        // NOTE(lmr): Without this, the value arguments don't seem to end up in the resolved call. I'm not
        // sure if there is a better way to do this or not but this seems to work okay.
        val setterCall = makeCallWithValueArguments(
            resolvedCall.call,
            listOf(CallMaker.makeValueArgument(valueExpr))
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

        return KtxAttributeInfo(
            name = descriptor.name.asString(),
            type = descriptor.type,
            descriptor = descriptor,
            setterResolvedCall = resolvedSetterCall,
            isIncludedInConstruction = false, // attributesInConstruction.contains(nameAsString),
            isPivotal = !descriptor.isVar // NOTE(lmr): i don't think this can happen... it wouldn't resolve here in this case
        )
    }
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