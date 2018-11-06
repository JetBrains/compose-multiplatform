package org.jetbrains.kotlin.r4a

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.r4a.analysis.R4ADefaultErrorMessages
import org.jetbrains.kotlin.r4a.analysis.R4AErrors
import org.jetbrains.kotlin.r4a.ast.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.checkers.UnderscoreUsageChecker
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.*
import org.jetbrains.kotlin.resolve.calls.tower.NewResolutionOldInference
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
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
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

/**
 *
 */
class KtxCallResolver(
    private val callResolver: CallResolver,
    private val facade: ExpressionTypingFacade,
    private val project: Project
) {
    // TODO(lmr): I am unsure of the performance consequences of using this. it appears to create a file for every
    // call, which seems like something we definitely do not want, but it's also used in resolving for(..) loops, so
    // maybe it's not terrible.
    private val psiFactory = KtPsiFactory(project, markGenerated = false)
    private val builtIns = DefaultBuiltIns.Instance
    private val composableAnnotationChecker = ComposableAnnotationChecker()

    // The type of the `composer` variable in scope of the KTX tag
    private lateinit var composerType: KotlinType
    // A ResolvedCall to "get" the composer variable in scope of the KTX element.
    private lateinit var composerResolvedCall: ResolvedCall<*>
    // A ResolvedCall to the `joinKey(Any, Any?)` method on the composer in scope.
    private lateinit var joinKeyCall: ResolvedCall<*>

    private lateinit var tagExpressions: List<KtExpression>

    // Set of valid upper bound types that were defined on the composer that can't have children
    // For android, this should be [View]
    private val emitSimpleUpperBoundTypes = mutableSetOf<KotlinType>()

    // Set of valid upper bound types that were defined on the composer that can have children.
    // For android, this would be [ViewGroup]
    private val emitCompoundUpperBoundTypes = mutableSetOf<KotlinType>()

    // The specification for `emit` on a composer allows for the `ctor` parameter to be a function type
    // with any number of parameters. We allow for these parameters to be used as parameters in the
    // Constructors that are emitted with a KTX tag. These parameters can be overridden with attributes
    // in the KTX tag, but if there are required parameters with a type that matches one declared in the
    // ctor parameter, we will resolve it automatically with the value passed in the `ctor` lambda.
    //
    // In order to do this resolution, we store a list of pairs of "upper bounds" to parameter types. For example,
    // the following emit call:
    //
    //      fun <T : View> emit(key: Any, ctor: (context: Context) -> T, update: U<T>.() -> Unit)
    //
    // would produce a Pair of [View] to [Context]
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
            // TODO(lmr): we should make sure a "smart import" or something pops up here
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
        class ROOT(val expression: KtExpression) : ResolveKind()
        class NESTED(
            val calleeType: KotlinType,
            val nonMemoizedCall: NonMemoizedCallNode?,
            val isStaticCall: Boolean,
            val parent: ResolveKind
        ) : ResolveKind()
    }

    private class TempResolveInfo(
        val valid: Boolean,
        val trace: TemporaryTraceAndCache,
        val attributesLeft: Int,
        val usedAttributes: Set<String>,
        val missingRequiredAttributes: List<DeclarationDescriptor>,
        val build: () -> EmitOrCallNode
    )

    private class AttributeInfo(
        val value: KtExpression,
        val key: KtSimpleNameExpression?,
        val name: String,
        val isChildren: Boolean
    ) {
        constructor(attr: KtxAttribute) : this(attr.value ?: attr.key!!, attr.key, attr.name!!, false)
        constructor(body: KtLambdaExpression) : this(body, null, CHILDREN_KEY, true)

        fun makeArgumentValue(name: String = this.name, named: Boolean = false): ValueArgument {
            if (!named) {
                return object : ValueArgument {
                    override fun getArgumentExpression() = value
                    override fun getArgumentName() = null
                    override fun isNamed() = false
                    override fun asElement(): KtElement = value
                    override fun getSpreadElement() = null
                    override fun isExternal() = true
                }
            }
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

    // TODO(lmr): this might be able to be combined with ResolveKind
    private class CyclePreventer(
        private val attributes: Set<String> = emptySet(),
        private val isValid: Boolean = true,
        private val trail: IntArray = intArrayOf(1, 1, 1)
    ) {

        fun recurse(
            possibleAttributes: Set<String>,
            specifiedAttributes: Set<String>
        ): CyclePreventer {

            // steps in the recursion cannot define attributes that conflict with previous steps
            val hasDuplicates = attributes.intersect(possibleAttributes).isNotEmpty()

            // we require that at least one of the last three steps has had an attribute that was used.
            // we could tweak this. Potentially the "hasDuplicates" test is more than enough to prevent
            // infinite recursion.
            val nextTrail = intArrayOf(trail[1], trail[2], specifiedAttributes.size)
            val trailIsValid = nextTrail.sum() > 0

            return CyclePreventer(
                attributes = attributes + possibleAttributes,
                isValid = !hasDuplicates && trailIsValid,
                trail = nextTrail
            )
        }

        fun canRecurse(): Boolean = isValid
    }

    /**
     *
     */
    fun resolveTag(
        element: KtxElement,
        tagExpressions: List<KtExpression>,
        attributes: Collection<KtxAttribute>,
        body: KtLambdaExpression?,
        context: ExpressionTypingContext
    ): ResolvedKtxElementCall? {
        val tmpTrace = TemporaryTraceAndCache.create(context, "trace for ktx tag", element)
        val openTagExpr = tagExpressions.first()
        val contextToUse = context.replaceTraceAndCache(tmpTrace)

        val receiver = resolveReceiver(openTagExpr, contextToUse)

        val attrInfos = mutableMapOf<String, AttributeInfo>()
        for (attr in attributes) {
            AttributeInfo(attr).let {
                if (attrInfos.contains(it.name)) {
                    contextToUse.trace.reportFromPlugin(
                        R4AErrors.DUPLICATE_ATTRIBUTE.on(it.key!!),
                        R4ADefaultErrorMessages
                    )
                } else {
                    attrInfos.put(it.name, it)
                }
            }
        }
        body?.let { AttributeInfo(it) }?.let { attrInfos.put(it.name, it) }

        attrInfos[TAG_KEY] = AttributeInfo(
            value = openTagExpr,
            name = TAG_KEY,
            isChildren = false,
            key = null
        )

        val usedAttributes = mutableSetOf<String>()

        val missingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

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
            ResolveKind.ROOT(openTagExpr),
            CyclePreventer(),
            call,
            attrInfos,
            usedAttributes,
            missingRequiredAttributes,
            contextToUse
        )

        // TODO(lmr): validate that if it bottoms out at an emit(...) that it doesn't have any call(...)s

        if (emitOrCall != null) {

            emitOrCall.errorNode()?.let {
                when (it) {
                    is ErrorNode.NonCallableRoot -> {
                        // or should we do "INVALID_TAG_TYPE" here?
                        // TODO(lmr): should we remove the "FUNCTION_EXPECTED" diagnostic and replace it with our own?
                        val typeInfo = facade.getTypeInfo(openTagExpr, context.withThrowawayTrace(openTagExpr))
                        val type = typeInfo.type

                        // TODO(lmr): check to see if type is abstract, report CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS

                        if (type != null) {
                            R4AErrors.INVALID_TAG_TYPE.report(
                                contextToUse,
                                tagExpressions,
                                type,
                                emitSimpleUpperBoundTypes
                            )
                        } else {
                            R4AErrors.INVALID_TAG_DESCRIPTOR.report(
                                contextToUse,
                                tagExpressions,
                                emitSimpleUpperBoundTypes
                            )
                        }
                    }
                    is ErrorNode.NonEmittableNonCallable -> {
                        // TODO(lmr): diagnostic
                        // "ktx tag terminated with type "Foo", which is neither an emittable, nor callable
                        R4AErrors.INVALID_TAG_TYPE.report(
                            contextToUse,
                            tagExpressions,
                            it.type,
                            emitSimpleUpperBoundTypes
                        )
                    }
                    is ErrorNode.RecursionLimitError -> {
                        R4AErrors.CALLABLE_RECURSION_DETECTED.report(contextToUse, tagExpressions)
                    }
                }
            }

            val constantChecker = ConstantExpressionEvaluator(
                project = project,
                module = contextToUse.scope.ownerDescriptor.module,
                languageVersionSettings = contextToUse.languageVersionSettings
            )

            val attributeNodes = emitOrCall
                .allAttributes()
                .mapNotNull { it as? AttributeNode }
                .groupBy { it.name }

            val usedAttributeNodes = attributeNodes
                .mapValues { it.value.first() }
                .values
                .map {
                    val attr = attrInfos[it.name] ?: error("couldnt find attribute ${it.name}")
                    val static = isStatic(attr.value, contextToUse, it.type, constantChecker)

                    // update all of the nodes in the AST
                    attributeNodes[it.name]?.forEach { it.isStatic = static }

                    // return a node for the root of the AST that codegen can use
                    AttributeNode(
                        name = it.name,
                        descriptor = it.descriptor,
                        expression = attr.value,
                        type = it.type,
                        isStatic = static
                    )
                }

            val unusedAttributes = (attrInfos - usedAttributes - TAG_KEY).toMutableMap()

            if (unusedAttributes.isNotEmpty()) {

                // if we have some unused attributes, we want to provide some helpful diagnostics
                val allPossibleAttributes = emitOrCall.allPossibleAttributes()

                loop@ for (attr in unusedAttributes.values) {
                    when (attr.name) {
                        CHILDREN_KEY -> {
                            if (emitOrCall is EmitCallNode) {
                                val type = emitOrCall.memoize.ctorCall?.resultingDescriptor?.returnType ?: continue@loop
                                if (!type.isCompoundEmittable()) {
                                    contextToUse.trace.reportFromPlugin(
                                        R4AErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(openTagExpr),
                                        R4ADefaultErrorMessages
                                    )
                                } else {
                                    unusedAttributes.remove(CHILDREN_KEY)
                                }
                            } else {
                                val possibleChildren = allPossibleAttributes[CHILDREN_KEY] ?: emptyList()
                                if (possibleChildren.isNotEmpty()) {
                                    contextToUse.trace.reportFromPlugin(
                                        R4AErrors.UNRESOLVED_CHILDREN.on(openTagExpr, possibleChildren.map { it.type }),
                                        R4ADefaultErrorMessages
                                    )
                                } else {
                                    contextToUse.trace.reportFromPlugin(
                                        R4AErrors.CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED.on(openTagExpr),
                                        R4ADefaultErrorMessages
                                    )
                                }
                            }
                        }
                        else -> {
                            val key = attr.key ?: error("expected non-null key expression")
                            // TODO(lmr): should this be a "throwaway" context?
                            val valueType = facade.getTypeInfo(attr.value, contextToUse).type

                            // TODO(lmr): add expected_type_info here?

                            val descriptors = emitOrCall.resolvedCalls().flatMap {
                                listOfNotNull(
                                    it.resultingDescriptor,
                                    it.resultingDescriptor.returnType?.let {
                                        if (it.isUnit()) null
                                        else it.constructor.declarationDescriptor
                                    }
                                )
                            }

                            val attrsOfSameKey = resolveAttributeCandidatesGivenNameAndNode(
                                emitOrCall,
                                attr.name,
                                context.withThrowawayTrace(openTagExpr)
                            )

                            val diagnostic = when {
                                attrsOfSameKey.isNotEmpty() && valueType != null ->
                                    R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(key, valueType, attrsOfSameKey.map { it.type })
                                attrsOfSameKey.isEmpty() && valueType != null ->
                                    R4AErrors.UNRESOLVED_ATTRIBUTE_KEY.on(key, descriptors, attr.name, valueType)
                                attrsOfSameKey.isNotEmpty() && valueType == null ->
                                    R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                        key,
                                        ErrorUtils.createErrorType("???"),
                                        attrsOfSameKey.map { it.type })
                                else ->
                                    R4AErrors.UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE.on(key, descriptors, attr.name)
                            }

                            contextToUse.trace.reportFromPlugin(diagnostic, R4ADefaultErrorMessages)
                        }
                    }
                }
            }

            if (missingRequiredAttributes.isNotEmpty()) {
                missingRequiredAttributes
                    .filter { !it.hasChildrenAnnotation() }
                    .ifNotEmpty {
                        R4AErrors.MISSING_REQUIRED_ATTRIBUTES.report(contextToUse, tagExpressions, this)
                    }
                missingRequiredAttributes
                    .filter { it.hasChildrenAnnotation() }
                    .ifNotEmpty {
                        // TODO(lmr): update this diagnostic to include the children type
                        R4AErrors.MISSING_REQUIRED_CHILDREN.report(contextToUse, tagExpressions)
                    }
            }

            tmpTrace.commit()

            return ResolvedKtxElementCall(
                usedAttributes = usedAttributeNodes,
                unusedAttributes = unusedAttributes.keys.toList(),
                emitOrCall = emitOrCall,
                getComposerCall = composerResolvedCall
            )
        }

        return null
    }

    private fun resolveAttributeCandidatesGivenNameAndNode(
        node: EmitOrCallNode,
        name: String,
        context: ExpressionTypingContext
    ): List<AttributeMeta> {
        val setterName = Name.identifier(R4aUtils.setterMethodFromPropertyName(name))
        val fakeSetterExpr = psiFactory.createSimpleName(setterName.asString())
        val fakePropertyExpr = psiFactory.createSimpleName(name)
        val contextToUse = context.replaceCollectAllCandidates(true)
        val resolvedCalls = node.resolvedCalls()

        val params = resolvedCalls
            .flatMap { it.resultingDescriptor.valueParameters }
            .filter { it.name.asString() == name }
            .mapNotNull {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        val types = resolvedCalls
            .mapNotNull { it.resultingDescriptor.returnType }
            .filter { !it.isUnit() }

        // setters, including extension setters
        val setters = types
            .flatMap { type ->
                val call = makeCall(
                    callElement = fakeSetterExpr,
                    calleeExpression = fakeSetterExpr,
                    receiver = TransientReceiver(type)
                )

                callResolver.resolveCallWithGivenName(
                    BasicCallResolutionContext.create(
                        contextToUse,
                        call,
                        CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(contextToUse.dataFlowInfo, call)
                    ),
                    call,
                    fakeSetterExpr,
                    setterName
                ).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? SimpleFunctionDescriptor }
            .mapNotNull {
                when {
                    it.valueParameters.size != 1 -> null
                    it.returnType?.isUnit() == false -> null
                    else -> AttributeMeta(
                        name = name,
                        type = it.valueParameters.first().type,
                        isChildren = it.hasChildrenAnnotation(),
                        descriptor = it
                    )
                }
            }

        val properties = types
            .flatMap { type ->
                val call = CallMaker.makePropertyCall(TransientReceiver(type), null, fakePropertyExpr)

                val contextForVariable = BasicCallResolutionContext.create(
                    contextToUse,
                    call,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
                )

                callResolver.resolveSimpleProperty(contextForVariable).allCandidates ?: emptyList()
            }
            .mapNotNull { it.resultingDescriptor as? PropertyDescriptor }
            .map {
                AttributeMeta(
                    name = name,
                    type = it.type,
                    isChildren = it.hasChildrenAnnotation(),
                    descriptor = it
                )
            }

        return params + setters + properties
    }

    private fun DeclarationDescriptor.isRoot() = containingDeclaration?.containingDeclaration is ModuleDescriptor

    private fun isValidStaticQualifiedPart(target: DeclarationDescriptor): Boolean {
        return when (target) {
            is ClassDescriptor -> when {
                target.kind == ClassKind.OBJECT -> true
                target.isCompanionObject -> true
                else -> false
            }
            is ClassConstructorDescriptor -> true
            is PropertyDescriptor -> when {
                target.isVar -> false
                target.isConst -> true
                target.getter?.isDefault == true -> true
                else -> false
            }
            is FieldDescriptor -> isValidStaticQualifiedPart(target.correspondingProperty)
            is SimpleFunctionDescriptor -> true
            else -> {
                false
            }
        }
    }

    private fun isStatic(kind: ResolveKind, resolvedCall: ResolvedCall<*>, context: ExpressionTypingContext): Boolean {
        return when (kind) {
            is ResolveKind.ROOT -> when (kind.expression) {
                is KtQualifiedExpression -> {
                    val parts = kind.expression.asQualifierPartList()
                    val targets = parts
                        .mapNotNull { it.expression }
                        .mapNotNull { context.trace[BindingContext.REFERENCE_TARGET, it] }

                    if (parts.size != targets.size) return false

                    val first = targets.first()

                    if (!first.isRoot()) return false

                    for (target in targets) {
                        val isValid = isValidStaticQualifiedPart(target)
                        if (!isValid)
                            return false
                        print(target)
                    }
                    // TODO(lmr):
                    return true
                }
                is KtSimpleNameExpression -> {
                    when (resolvedCall) {
                        is VariableAsFunctionResolvedCall -> {
                            val variableDescriptor = resolvedCall.variableCall.candidateDescriptor
                            if (variableDescriptor.isVar) return false
                            if (variableDescriptor.isConst) return true
                            val isRoot = variableDescriptor.isRoot()
                            when (variableDescriptor) {
                                is PropertyDescriptor -> (variableDescriptor.getter?.isDefault ?: false) && isRoot
                                else -> false
                            }
                        }
                        else -> true
                    }
                }
                else -> false
            }
            is ResolveKind.NESTED -> kind.isStaticCall
        }
    }

    private fun shouldMemoizeResult(resolvedCall: ResolvedCall<*>): Boolean {
        val descriptor = resolvedCall.resultingDescriptor
        val returnType = descriptor.returnType ?: builtIns.unitType
        val typeDescriptor = returnType.constructor.declarationDescriptor
        return returnType.hasMemoizableAnnotation() ||
                descriptor.hasMemoizableAnnotation() ||
                (typeDescriptor?.hasMemoizableAnnotation() ?: false)
    }

    private fun constructNonMemoizedCallLinkedList(kind: ResolveKind, nonMemoizedCall: NonMemoizedCallNode): NonMemoizedCallNode {
        var call = nonMemoizedCall
        var node = kind
        while (node is ResolveKind.NESTED) {
            val prevCall = node.nonMemoizedCall ?: break
            node = node.parent
            call = NonMemoizedCallNode(
                resolvedCall = prevCall.resolvedCall,
                params = prevCall.params,
                nextCall = call
            )
        }
        return call
    }

    private fun constructNonMemoizedCallLinkedList(kind: ResolveKind.NESTED): NonMemoizedCallNode? {
        return kind.nonMemoizedCall?.let { constructNonMemoizedCallLinkedList(kind.parent, it) }
    }

    private fun resolveTagValidations(
        kind: ResolveKind,
        isStaticCall: Boolean,
        expression: KtExpression,
        resolvedCall: ResolvedCall<*>,
        receiverScope: KotlinType,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (kind !is ResolveKind.ROOT) return emptyList()
        if (isStaticCall) return emptyList()
        val descriptor = resolvedCall.resultingDescriptor
        when (resolvedCall.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                val receiver = resolvedCall.dispatchReceiver as? ExpressionReceiver ?: return emptyList()
                return listOf(
                    ValidatedAssignment(
                        validationType = ValidationType.CHANGED,
                        validationCall = resolveValidationCall(
                            validationType = ValidationType.CHANGED,
                            attrType = receiver.type,
                            expressionToReportErrorsOn = receiver.expression,
                            receiverScope = receiverScope,
                            valueExpr = receiver.expression,
                            context = context
                        ),
                        assignment = null,
                        attribute = AttributeNode(
                            name = TAG_KEY,
                            isStatic = false,
                            type = receiver.type,
                            expression = receiver.expression,
                            descriptor = descriptor
                        )
                    )
                )
            }
            else -> return emptyList()
        }
    }

    private fun collectValidations(
        current: List<ValidatedAssignment>,
        children: List<AttributeNode>,
        expression: KtExpression,
        invalidReceiverScope: KotlinType?,
        attributes: Map<String, AttributeInfo>,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        val result = mutableMapOf<String, ValidatedAssignment>()

        current.forEach {
            result[it.attribute.name] = it
        }

        children.forEach {
            // TODO(lmr): how do we know we want the one that's already in there?
            if (result.containsKey(it.name)) return@forEach
            val attr = attributes[it.name] ?: error("didnt find attribute")
            result[it.name] = it.asChangedValidatedAssignment(
                expressionToReportErrorsOn = attr.key ?: expression,
                receiverScope = invalidReceiverScope,
                valueExpr = attr.value,
                context = context
            )
        }

        return result.values.toList()
    }

    private fun resolveThing(
        expression: KtExpression,
        kind: ResolveKind,
        cyclePreventer: CyclePreventer,
        call: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): EmitOrCallNode? {
        if (!cyclePreventer.canRecurse()) {
            return when (kind) {
                is ResolveKind.ROOT -> error("should never happen")
                is ResolveKind.NESTED -> {
                    // TODO(lmr): cyclePreventer.recursionLimitMessage?
                    val error = ErrorNode.RecursionLimitError()
                    constructNonMemoizedCallLinkedList(kind)?.apply { nextCall = error } ?: error
                }
            }
        }
        val tmpForCandidates = TemporaryTraceAndCache.create(
            context, "trace to resolve ktx element", expression
        )
        val results = getCandidates(kind, call, context.replaceTraceAndCache(tmpForCandidates))

        if (results.isNothing) {
            return when (kind) {
                is ResolveKind.ROOT -> {
                    // some of the diagnostics that getCandidates() will put on the PSI are useful, so we commit the
                    // trace here, but only in the case of this being the "root" call.
                    tmpForCandidates.commit()
                    ErrorNode.NonCallableRoot()
                }
                is ResolveKind.NESTED -> {
                    val error = ErrorNode.NonEmittableNonCallable(kind.calleeType)
                    constructNonMemoizedCallLinkedList(kind)?.apply { nextCall = error } ?: error
                }
            }
        }

        // TODO(lmr): we could have an optimization for results.isSuccess and attributes.size == 0 here

        val resolveInfos = results.resultingCalls.mapNotNull { result ->
            val tmpForCandidate = TemporaryTraceAndCache.create(
                context, "trace to resolve ktx element", expression
            )

            val candidateContext = context.replaceTraceAndCache(tmpForCandidate)

            val attrsUsedInCall = mutableSetOf<String>()

            val attrsUsedInSets = mutableSetOf<String>()

            val subMissingRequiredAttributes = mutableListOf<DeclarationDescriptor>()

            val usedAttributeInfos = mutableListOf<TempParameterInfo>()

            val candidateResults = resolveCandidate(
                kind,
                result,
                call,
                attributes,
                attrsUsedInCall,
                usedAttributeInfos,
                subMissingRequiredAttributes,
                candidateContext
            )

            // TODO(lmr): in the case of null, do we want to return some "ErrorNode" of some sort and report a reasonable diagnostic?
            if (candidateResults.isNothing) return@mapNotNull null

            val resolvedCall = candidateResults.resultingCalls.first()

            if (!candidateResults.isSuccess) {
                when (candidateResults.resultCode) {
                    OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH -> {
                        resolvedCall.call.valueArguments.map { resolvedCall.getArgumentMapping(it) }.forEach {
                            when (it) {
                                is ArgumentMatch -> {
                                    when (it.status) {
                                        ArgumentMatchStatus.TYPE_MISMATCH -> {
                                            val attr = attributes[it.valueParameter.name.asString()] ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type = facade.getTypeInfo(attr.value, candidateContext).type ?: return@forEach
                                            candidateContext.trace.reportFromPlugin(
                                                R4AErrors.MISMATCHED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                R4ADefaultErrorMessages
                                            )
                                        }
                                        ArgumentMatchStatus.ARGUMENT_HAS_NO_TYPE -> {
                                            error("ARGUMENT_HAS_NO_TYPE")
                                        }
                                        ArgumentMatchStatus.MATCH_MODULO_UNINFERRED_TYPES -> {
                                            val attr = attributes[it.valueParameter.name.asString()] ?: return@forEach
                                            val key = attr.key ?: return@forEach
                                            val type = facade.getTypeInfo(attr.value, candidateContext).type ?: return@forEach

                                            candidateContext.trace.reportFromPlugin(
                                                R4AErrors.MISMATCHED_INFERRED_ATTRIBUTE_TYPE.on(
                                                    key,
                                                    type,
                                                    listOfNotNull(it.valueParameter.type)
                                                ),
                                                R4ADefaultErrorMessages
                                            )
                                        }
                                        ArgumentMatchStatus.UNKNOWN -> {
                                            // NOTE(lmr): This can happen with the implicit constructor params. ignore
                                        }
                                        ArgumentMatchStatus.SUCCESS -> {
                                            // do nothing
                                        }
                                    }
                                }
                                is ArgumentUnmapped -> {
                                    error("argument unmapped...")
                                }
                            }
                        }
                    }
                    OverloadResolutionResults.Code.INCOMPLETE_TYPE_INFERENCE -> {
                        error("INCOMPLETE_TYPE_INFERENCE")
                    }
                    else -> {
                        error("new kind of resolution problem. figure out why this happened...")
                    }
                }
            }

            val returnType = resolvedCall.resultingDescriptor.returnType ?: builtIns.unitType

            val isStaticCall = isStatic(kind, resolvedCall, candidateContext)

            val shouldMemoizeCtor = shouldMemoizeResult(resolvedCall)

            val nonMemoizedCall = NonMemoizedCallNode(
                resolvedCall = resolvedCall,
                params = constructAttributeNodes(resolvedCall, attributes),
                nextCall = null
            )

            if (returnType.isUnit()) {
                // bottomed out

                // it is important to pass in "result" here and not "resolvedCall" since "result" is the one that will have
                // the composable annotation on it in the case of lambda invokes
                val composability = composableAnnotationChecker.hasComposableAnnotation(candidateContext.trace, result)
                if (composability == ComposableAnnotationChecker.Composability.NOT_COMPOSABLE) {
                    candidateContext.trace.reportFromPlugin(
                        R4AErrors.NON_COMPOSABLE_INVOCATION.on(
                            expression,
                            "Lambda variable", // TODO(lmr): this diagnostic could accept descriptor directly
                            resolvedCall.candidateDescriptor.name.asString()
                        ),
                        R4ADefaultErrorMessages
                    )
                }

                return@mapNotNull TempResolveInfo(
                    true, // TODO(lmr): valid
                    tmpForCandidate,
                    (attributes - attrsUsedInCall).size,
                    attrsUsedInCall,
                    subMissingRequiredAttributes
                ) {
                    val composerCall = resolveComposerCall(
                        constructedType = null, // or should we pass in Unit here?
                        expressionToReportErrorsOn = expression,
                        context = candidateContext
                    )

                    // the "invalid" lambda is at a different argument index depending on whether or not there is a "ctor" param.
                    val invalidArgIndex = 1

                    val invalidReceiverScope = composerCall?.let {
                        it.resultingDescriptor.valueParameters.getOrNull(invalidArgIndex)?.type?.getReceiverTypeFromFunctionType()
                    }

                    val tagValidations = resolveTagValidations(
                        kind = kind,
                        isStaticCall = isStaticCall,
                        expression = expression,
                        resolvedCall = resolvedCall,
                        receiverScope = invalidReceiverScope!!,
                        context = candidateContext
                    )

                    val pivotals = resolvePivotalAttributes(
                        attributes,
                        attrsUsedInCall,
                        usedAttributeInfos,
                        emptyList(),
                        returnType
                    )

                    if (kind is ResolveKind.ROOT) {
                        MemoizedCallNode(
                            memoize = ComposerCallInfo(
                                composerCall = composerCall,
                                functionDescriptors = constructLambdaArgumentsFunctionConstructors(composerCall, candidateContext),
                                pivotals = pivotals,
                                joinKeyCall = joinKeyCall,
                                ctorCall = null,
                                ctorParams = emptyList(),
                                validations = collectValidations(
                                    current = tagValidations,
                                    children = nonMemoizedCall.consumedAttributes(),
                                    expression = expression,
                                    attributes = attributes,
                                    invalidReceiverScope = invalidReceiverScope,
                                    context = candidateContext
                                )
                            ),
                            call = constructNonMemoizedCallLinkedList(kind, nonMemoizedCall)
                        )
                    } else {
                        constructNonMemoizedCallLinkedList(kind, nonMemoizedCall)
                    }
                }
            }

            if (returnType.isEmittable()) {

                val composerCall = resolveComposerEmit(
                    constructedType = returnType,
                    hasBody = attributes.contains(CHILDREN_KEY),
                    implicitCtorTypes = resolvedCall.call.valueArguments.mapNotNull { (it as? ImplicitCtorValueArgument)?.type },
                    expressionToReportErrorsOn = expression,
                    context = candidateContext
                )

                if (attributes.contains(CHILDREN_KEY) && returnType.isCompoundEmittable()) {
                    attrsUsedInSets.add(CHILDREN_KEY)
                }

                val updateArgIndex = 2

                val updateReceiverScope = composerCall?.let {
                    it.resultingDescriptor.valueParameters.getOrNull(updateArgIndex)?.type?.getReceiverTypeFromFunctionType()
                }

                val setterValidations = resolveAllSetAttributes(
                    expression,
                    updateReceiverScope,
                    returnType,
                    attributes,
                    attrsUsedInCall,
                    attrsUsedInSets,
                    subMissingRequiredAttributes,
                    shouldMemoizeCtor,
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
                    (attributes - attrsUsedInCall - attrsUsedInSets).size,
                    attrsUsedInCall + attrsUsedInSets,
                    subMissingRequiredAttributes
                ) {
                    EmitCallNode(
                        memoize = ComposerCallInfo(
                            composerCall = composerCall,
                            functionDescriptors = constructLambdaArgumentsFunctionConstructors(composerCall, candidateContext),
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
                context = candidateContext
            )

            // the "invalid" lambda is at a different argument index depending on whether or not there is a "ctor" param.
            val invalidArgIndex = 2

            val invalidReceiverScope = composerCall?.let {
                it.resultingDescriptor.valueParameters.getOrNull(invalidArgIndex)?.type?.getReceiverTypeFromFunctionType()
            }

            val tagValidations = resolveTagValidations(
                kind = kind,
                isStaticCall = isStaticCall,
                expression = expression,
                resolvedCall = resolvedCall,
                receiverScope = invalidReceiverScope!!,
                context = candidateContext
            )

            val setterValidations = resolveAllSetAttributes(
                expression,
                invalidReceiverScope,
                returnType,
                attributes,
                attrsUsedInCall,
                attrsUsedInSets,
                subMissingRequiredAttributes,
                shouldMemoizeCtor,
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

            val childCall = resolveThing(
                expression,
                ResolveKind.NESTED(
                    returnType,
                    if (shouldMemoizeCtor) null else nonMemoizedCall,
                    isStaticCall,
                    kind
                ),
                cyclePreventer.recurse(
                    resolvedCall.resultingDescriptor.valueParameters.map { it.name.asString() }.toSet(),
                    attrsUsedInCall + attrsUsedInSets
                ),
                makeCall(
                    call.callElement,
                    calleeExpression = call.calleeExpression,
                    dispatchReceiver = TransientReceiver(returnType)
                ),
                attributes,
                attrsUsedInFollowingCalls,
                subMissingRequiredAttributes,
                candidateContext
            )

            // TODO(lmr): is canceling early here the right thing to do? what about in the case of emit(...)?
            // TODO(lmr): if childcall returned null, we should add an "INVALID_TAG_TYPE" diagnostic...
            // TODO(lmr): we probably shouldn't let childCall return null? makes error reporting harder....
            if (childCall == null) return@mapNotNull null

            val subUsedAttributes = attrsUsedInCall + attrsUsedInSets + attrsUsedInFollowingCalls

            val attrsLeft = (attributes - subUsedAttributes).size

            // TODO(lmr): if we have 0 attributes left and it's a success, we can go ahead and return early here

            return@mapNotNull TempResolveInfo(
                true, // TODO(lmr): valid
                tmpForCandidate,
                attrsLeft,
                subUsedAttributes,
                subMissingRequiredAttributes
            ) {
                if (shouldMemoizeCtor || kind is ResolveKind.ROOT) {
                    MemoizedCallNode(
                        memoize = ComposerCallInfo(
                            composerCall = composerCall,
                            functionDescriptors = constructLambdaArgumentsFunctionConstructors(composerCall, candidateContext),
                            pivotals = pivotals,
                            joinKeyCall = joinKeyCall,
                            ctorCall = if (shouldMemoizeCtor) nonMemoizedCall.resolvedCall else null,
                            ctorParams = if (shouldMemoizeCtor) nonMemoizedCall.params else emptyList(),
                            validations = collectValidations(
                                current = tagValidations + setterValidations,
                                children = childCall.consumedAttributes(),
                                expression = expression,
                                attributes = attributes,
                                invalidReceiverScope = invalidReceiverScope,
                                context = candidateContext
                            )
                        ),
                        call = childCall
                    )
                } else {
                    childCall
                }

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
            // TODO(lmr): should we check to see if another resolveInfo has same # of attributes used or something?
            val resultNode = result.build()
            usedAttributes.addAll(result.usedAttributes)
            missingRequiredAttributes.addAll(result.missingRequiredAttributes)
            result.trace.commit()
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

    private fun constructAttributeNodes(resolvedCall: ResolvedCall<*>, attributes: Map<String, AttributeInfo>): List<ValueNode> {
        return resolvedCall.valueArguments.mapNotNull { (param, value) ->
            // TODO(lmr): we should handle this case in a few other areas as well
            if (value is DefaultValueArgument) {
                return@mapNotNull DefaultValueNode(
                    name = param.name.asString(),
                    descriptor = param,
                    type = param.type
                )
            }

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
                name = attr.name,
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
        type: KotlinType?,
        attributes: Map<String, AttributeInfo>,
        attributesUsedInCall: Set<String>,
        consumedAttributes: MutableSet<String>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        shouldMemoizeCtor: Boolean,
        context: ExpressionTypingContext
    ): List<ValidatedAssignment> {
        if (type == null) return emptyList()
        val results = mutableListOf<ValidatedAssignment>()
        var children: AttributeInfo? = null
        for ((name, attribute) in attributes) {
            if (name == TAG_KEY) continue
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
                    !shouldMemoizeCtor && attributesUsedInCall.contains(name) -> ValidationType.CHANGED
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

                val descriptor = resolvedCall.resultingDescriptor

                val validationType = when {
                    attributesUsedInCall.contains(CHILDREN_KEY) -> ValidationType.UPDATE
                    else -> ValidationType.SET
                }

                val attrName = when (descriptor) {
                    is SimpleFunctionDescriptor -> R4aUtils.propertyNameFromSetterMethod(descriptor.name.asString())
                    is PropertySetterDescriptor -> descriptor.correspondingProperty.name.asString()
                    else -> descriptor.name.asString()
                }

                attributes[attrName]?.let {
                    // they are providing a named attribute for a @Children attribute while also providing a children
                    // body. This is illegal.
                    context.trace.reportFromPlugin(
                        R4AErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(it.key!!, attrName),
                        R4ADefaultErrorMessages
                    )
                    consumedAttributes.add(attrName)
                }

                val attrType = descriptor.valueParameters.first().type

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
                            isStatic = false
                        ),
                        validationCall = validationCall
                    )
                )
                consumedAttributes.add(CHILDREN_KEY)
            }
        }

        if (!type.isUnit()) {
            val cls = type.constructor.declarationDescriptor as? ClassDescriptor ?: error("unexpected classifier descriptor")
            val requiredAttributes = cls.unsubstitutedMemberScope
                .getContributedDescriptors()
                .mapNotNull { it as? PropertyDescriptor }
                .filter { it.isLateInit }

            requiredAttributes
                .filter { !consumedAttributes.contains(it.name.asString()) }
                .filter { !it.hasChildrenAnnotation() }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }

            requiredAttributes
                .filter { it.hasChildrenAnnotation() }
                .filter { !consumedAttributes.contains(it.name.asString()) && !consumedAttributes.contains(CHILDREN_KEY) }
                .ifNotEmpty { missingRequiredAttributes.addAll(this) }
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
        candidate: ResolvedCall<FunctionDescriptor>,
        original: Call,
        attributes: Map<String, AttributeInfo>,
        usedAttributes: MutableSet<String>,
        usedAttributeInfos: MutableList<TempParameterInfo>,
        missingRequiredAttributes: MutableList<DeclarationDescriptor>,
        context: ExpressionTypingContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val valueArguments = mutableListOf<ValueArgument>()

        val referencedDescriptor = candidate.resultingDescriptor

        val stableParamNames = referencedDescriptor.hasStableParameterNames()

        for (param in referencedDescriptor.valueParameters) {
            val name = param.name.asString()
            val attr = attributes[name]
            var arg: ValueArgument? = null

            if (arg == null && isAnnotatedAsChildren(param)) {
                val childrenAttr = attributes[CHILDREN_KEY]
                if (childrenAttr != null) {
                    usedAttributes.add(CHILDREN_KEY)
                    arg = childrenAttr.makeArgumentValue(name, stableParamNames)

                    if (attr != null) {
                        // they are providing a named attribute for a @Children attribute while also providing a children
                        // body. This is illegal.
                        context.trace.reportFromPlugin(
                            R4AErrors.CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE.on(attr.key!!, attr.name),
                            R4ADefaultErrorMessages
                        )
                        usedAttributes.add(attr.name)
                    }
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
                arg = attr.makeArgumentValue(attr.name, stableParamNames)
            }

            if (arg == null && isImplicitConstructorParam(param, referencedDescriptor)) {
                arg = ImplicitCtorValueArgument(param.type)
            }

            if (arg != null) {
                valueArguments.add(arg)
            } else if (!param.declaresDefaultValue()) {
                // missing required parameter!
                missingRequiredAttributes.add(param)
            }
        }

        val call = makeCall(
            original.callElement,
            valueArguments = valueArguments,
            calleeExpression = original.calleeExpression,
            receiver = original.explicitReceiver,
            dispatchReceiver = original.dispatchReceiver
        )

        // We have to be somewhat careful here. The ControlFlowProcessor seems to fall into an infinite loop if we resolve a
        // VariableAsFunctionResolvedCall with the resolveCallWithKnownCandidate API, so we explicitly go down a different path in
        // those cases here. There might be a better way of doing this long term, but this seemed to produce the results we want.
        if (candidate is VariableAsFunctionResolvedCall) {
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

        // resolver just needs a KtReferenceExpression to store the REFERENCE_TARGET on, but we are going to do that ourselves
        // so we just pass in a fake element here
        val expr = original.calleeExpression as? KtReferenceExpression ?: psiFactory.createSimpleName("fake")

        val result = callResolver.resolveCallWithKnownCandidate(
            call,
            TracingStrategyImpl.create(expr, call),
            BasicCallResolutionContext.create(
                context,
                call,
                CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
            ),
            ResolutionCandidate.create(
                call,
                candidate.candidateDescriptor,
                candidate.dispatchReceiver,
                candidate.explicitReceiverKind,
                null
            ),
            DataFlowInfoForArgumentsImpl(context.dataFlowInfo, call)
        )

        return result
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

    private fun createFunctionDescriptor(
        type: KotlinType,
        context: ExpressionTypingContext
    ): FunctionDescriptor {
        return AnonymousFunctionDescriptor(
            context.scope.ownerDescriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE,
            false
        ).apply {
            initialize(
                type.getReceiverTypeFromFunctionType()?.let {
                    DescriptorFactory.createExtensionReceiverParameterForCallable(this, it, Annotations.EMPTY)
                },
                null,
                emptyList(),
                type.getValueParameterTypesFromFunctionType().mapIndexed { i, t ->
                    ValueParameterDescriptorImpl(
                        containingDeclaration = this,
                        original = null,
                        index = i,
                        annotations = Annotations.EMPTY,
                        name = Name.identifier("p$i"),
                        outType = t.type,
                        declaresDefaultValue = false,
                        isCrossinline = false,
                        isNoinline = false,
                        varargElementType = null,
                        source = SourceElement.NO_SOURCE
                    )
                },
                type.getReturnTypeFromFunctionType(),
                Modality.FINAL,
                Visibilities.DEFAULT_VISIBILITY,
                null
            )
            isOperator = false
            isInfix = false
            isExternal = false
            isInline = true
            isTailrec = false
            isSuspend = false
            isExpect = false
            isActual = false
        }
    }

    private fun constructLambdaArgumentsFunctionConstructors(
        resolvedCall: ResolvedCall<*>?,
        context: ExpressionTypingContext
    ): List<FunctionDescriptor?> {
        return resolvedCall?.resultingDescriptor?.valueParameters?.map {
            if (it.type.isFunctionType) createFunctionDescriptor(it.type, context)
            else null
        } ?: emptyList()
    }

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

    private fun forceResolveCallForInvoke(
        calleeType: KotlinType,
        context: BasicCallResolutionContext
    ): OverloadResolutionResults<FunctionDescriptor> {
        val fake = psiFactory.createSimpleName("invoke")
        val expressionReceiver = ExpressionReceiver.create(fake, calleeType, context.trace.bindingContext)
        val call = CallTransformer.CallForImplicitInvoke(
            context.call.explicitReceiver, expressionReceiver, context.call,
            false
        )
        val tracingForInvoke = TracingStrategyForInvoke(fake, call, calleeType)
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

val CHILDREN_KEY = "<children>"
private val TAG_KEY = "<tag>"

private val COMPOSABLE_FQNAME = R4aUtils.r4aFqName("Composable")
private val PIVOTAL_FQNAME = R4aUtils.r4aFqName("Pivotal")
private val CHILDREN_FQNAME = R4aUtils.r4aFqName("Children")
private val MEMOIZABLE_FQNAME = R4aUtils.r4aFqName("Memoized")
private val EMITTABLE_FQNAME = R4aUtils.r4aFqName("Emittable")

private val COMPOSER_NAME = Name.identifier("composer")
private val EMIT_NAME = Name.identifier("emit")
private val CALL_NAME = Name.identifier("call")
private val JOINKEY_NAME = Name.identifier("joinKey")
private val CTOR_PARAMETER_NAME = Name.identifier("ctor")
private val CHILDREN_PARAMETER_NAME = Name.identifier("children")

fun Annotated.hasChildrenAnnotation(): Boolean = annotations.findAnnotation(CHILDREN_FQNAME) != null
private fun Annotated.hasMemoizableAnnotation(): Boolean = annotations.findAnnotation(MEMOIZABLE_FQNAME) != null

private fun ExpressionTypingContext.withThrowawayTrace(expression: KtExpression) = replaceTraceAndCache(
    TemporaryTraceAndCache.create(
        this,
        "Throwaway trace and cache",
        expression
    )
)

private fun <E : PsiElement> DiagnosticFactory0<E>.report(context: ExpressionTypingContext, elements: Collection<E>) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it),
            R4ADefaultErrorMessages
        )
    }
}

private fun <E : PsiElement, T1> DiagnosticFactory1<E, T1>.report(context: ExpressionTypingContext, elements: Collection<E>, value1: T1) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1),
            R4ADefaultErrorMessages
        )
    }
}

private fun <E : PsiElement, T1, T2> DiagnosticFactory2<E, T1, T2>.report(context: ExpressionTypingContext, elements: Collection<E>, value1: T1, value2: T2) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1, value2),
            R4ADefaultErrorMessages
        )
    }
}

private fun <E : PsiElement, T1, T2, T3> DiagnosticFactory3<E, T1, T2, T3>.report(context: ExpressionTypingContext, elements: Collection<E>, value1: T1, value2: T2, value3: T3) {
    elements.forEach {
        context.trace.reportFromPlugin(
            on(it, value1, value2, value3),
            R4ADefaultErrorMessages
        )
    }
}