package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.StatementGenerator
import org.jetbrains.kotlin.psi2ir.generators.pregenerateCallReceivers
import org.jetbrains.kotlin.r4a.analysis.R4AWritableSlices
import org.jetbrains.kotlin.r4a.ast.*
import org.jetbrains.kotlin.r4a.frames.buildWithScope
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class ComposerSyntheticExtension : SyntheticIrExtension {
    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val resolvedKtxCall = statementGenerator.context.bindingContext.get(R4AWritableSlices.RESOLVED_KTX_CALL, element)
            ?: error("KTX Element encountered without a resolved KTX call. Something must have gone wrong in type resolution.")

        val irBuiltIns = statementGenerator.context.irBuiltIns

        val openTagName = element.simpleTagName ?: element.qualifiedTagName ?: error("malformed element")
        val resolvedAttributes = resolvedKtxCall.usedAttributes.map { it.name to it }.toMap()

        val statements = mutableListOf<IrStatement>()

        // Convert tag attribute expressions
        val attributeExpressions = resolvedAttributes.mapValues { (_, attribute) ->
            val valueExpression = attribute.expression
            val irValueExpression = KtPsiUtil.safeDeparenthesize(valueExpression).accept(statementGenerator, null) as? IrExpression
                ?: error("attributes need to be expressions")

            if (irValueExpression is IrConst<*>) {
                // For constant expression don't create the temporary.
                irValueExpression
            } else {
                // Create a temporary variable to hold the value of the expression
                val attributeType = attribute.type
                val attributeVariable = IrTemporaryVariableDescriptorImpl(
                    statementGenerator.scopeOwner,
                    Name.identifier("__el_attr_${attribute.name}"),
                    attributeType,
                    false
                )

                val attrVariableDeclaration = statementGenerator.context.symbolTable.declareVariable(
                    valueExpression.startOffset, valueExpression.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    attributeVariable,
                    attributeType.toIrType()!!,
                    irValueExpression
                )
                // OUTPUT: var _el_attr_name = <attribute expression>
                statements.add(attrVariableDeclaration)

                val getValue = IrGetValueImpl(
                    valueExpression.startOffset, valueExpression.endOffset,
                    statementGenerator.context.symbolTable.referenceVariable(attributeVariable)
                )

                getValue
            }
        }.toMap()

        fun getAttribute(name: String) = attributeExpressions[name] ?: error("Expected attribute $name")

        // TODO(lmr): if this is a "get local variable" will this still work?
        val getComposer = statementGenerator.getProperty(
            openTagName.startOffset, openTagName.endOffset,
            resolvedKtxCall.getComposerCall
        )

        fun keyExpression(callInfo: ComposerCallInfo): IrExpression {
            val sourceKey = getKeyValue(statementGenerator.scopeOwner, element.startOffset)
            return listOf<IrExpression>(
                IrConstImpl.int(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    irBuiltIns.intType,
                    sourceKey
                )
            )
                .append(callInfo.pivotals.map { getAttribute(it.name) })
                .reduce { left, right ->
                    statementGenerator
                        .callMethod(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            callInfo.joinKeyCall,
                            getComposer
                        )
                        .apply {
                            putValueArgument(0, left)
                            putValueArgument(1, right)
                        }
                }
        }

        fun lambdaExpression(
            descriptor: FunctionDescriptor,
            kotlinType: KotlinType,
            block: (statements: MutableList<IrStatement>) -> Unit
        ) = statementGenerator.lambdaExpression(
            openTagName.startOffset,
            openTagName.endOffset,
            descriptor,
            kotlinType,
            block
        )


        fun generateEmitOrCallNode(callNode: EmitOrCallNode, container: DeclarationDescriptor, receiver: IrExpression? = null): IrExpression {
            when (callNode) {
                is EmitCallNode -> {
                    val memoize = callNode.memoize
                    val composerCall = memoize.composerCall

                    val composerCallParameters = composerCall.resultingDescriptor.valueParameters.map { it.name to it }.toMap()
                    val composerCallParametersType = composerCall.resultingDescriptor.valueParameters.map { it.name to it.type }.toMap()

                    return statementGenerator.callMethod(
                        element.startOffset,
                        element.endOffset,
                        composerCall,
                        getComposer
                    ).apply {
                        // Place the key parameter
                        putValueArgument(composerCallParameters[KtxNameConventions.EMIT_KEY_PARAMETER]!!, keyExpression(memoize))

                        // Place the ctor parameter
                        val ctor = memoize.ctorCall!! // NOTE: this is always non-null for EmitCallNode
                        val ctorLambdaDescriptor = memoize.emitCtorFnDescriptor!!
                        val ctorValueParameters = ctorLambdaDescriptor.valueParameters
                        val ctorParameterType = composerCallParametersType[KtxNameConventions.EMIT_CTOR_PARAMETER]!!
                        val ctorLambda = lambdaExpression(ctorLambdaDescriptor, ctorParameterType) { statements ->
                            val ctorCall = statementGenerator.buildCall(
                                openTagName.startOffset,
                                openTagName.endOffset,
                                ctor,
                                ctor.resultingDescriptor.original as FunctionDescriptor
                            ).apply {
                                putValueParameters(memoize.ctorParams, statementGenerator, ctorValueParameters) { getAttribute(it) }
                            }
                            // OUTPUT: return <ctor>(<arguments>)
                            val scopeOwner = statementGenerator.context.symbolTable.referenceFunction(ctorLambdaDescriptor)
                            statements.add(
                                IrReturnImpl(
                                    openTagName.startOffset, openTagName.endOffset,
                                    statementGenerator.context.irBuiltIns.nothingType,
                                    scopeOwner, ctorCall
                                )
                            )
                        }
                        putValueArgument(composerCallParameters[KtxNameConventions.EMIT_CTOR_PARAMETER]!!, ctorLambda)

                        // Place the updater parameter
                        val updaterLambdaDescriptor = memoize.emitUpdaterFnDescriptor!!
                        val updaterParameterType = composerCallParametersType[KtxNameConventions.EMIT_UPDATER_PARAMETER]!!
                        val updaterLambda = lambdaExpression(updaterLambdaDescriptor, updaterParameterType) { statements ->
                            statements.addAll(
                                memoize.validations.map { validation ->
                                    statementGenerator.validationCall(
                                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        validation,
                                        updaterLambdaDescriptor,
                                        validation.assignmentLambda?.extensionReceiverParameter ?: error("expected extension receiver")
                                    ) { name ->
                                        getAttribute(name)
                                    }
                                }
                            )

                            statementGenerator.addReturn(statements, updaterLambdaDescriptor)
                        }
                        putValueArgument(composerCallParameters[KtxNameConventions.EMIT_UPDATER_PARAMETER]!!, updaterLambda)

                        val bodyStatements = element.body
                        if (bodyStatements != null) {
                            // Place the children parameter
                            val bodyLambdaDescriptor = memoize.emitBodyFnDescriptor!!
                            val bodyParameterType = composerCallParametersType[KtxNameConventions.EMIT_CHILDREN_PARAMETER]!!
                            val bodyLambda = lambdaExpression(bodyLambdaDescriptor, bodyParameterType) { statements ->
                                statements.addAll(
                                    bodyStatements.map { it.accept(statementGenerator, null) }
                                )
                            }
                            putValueArgument(composerCallParameters[KtxNameConventions.EMIT_CHILDREN_PARAMETER]!!, bodyLambda)
                        }
                    }
                }
                is NonMemoizedCallNode -> {
                    var result: IrExpression = statementGenerator.buildCall(
                        element.startOffset,
                        element.endOffset,
                        resolvedCall = callNode.resolvedCall,
                        descriptor = callNode.resolvedCall.resultingDescriptor as FunctionDescriptor,
                        dispatchReceiver = receiver
                    ).apply {
                        putValueParameters(callNode.params, statementGenerator) { getAttribute(it) }
                    }

                    if (callNode.postAssignments.isNotEmpty()) {
                        //     Foo()
                        // turns into
                        //     val x = Foo()
                        //     x.prop = value
                        //     x
                        val elType = callNode.resolvedCall.resultingDescriptor.returnType!!

                        val elVarDescriptor = IrTemporaryVariableDescriptorImpl(
                            container,
                            Name.identifier("__el"),
                            elType,
                            false
                        )

                        val statements = mutableListOf<IrStatement>()

                        val elVarDecl = statementGenerator.context.symbolTable.declareVariable(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                            elVarDescriptor,
                            elType.toIrType()!!,
                            result
                        )

                        statements.add(elVarDecl)

                        val getEl = IrGetValueImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            statementGenerator.context.symbolTable.referenceVariable(elVarDescriptor)
                        )

                        for (assignment in callNode.postAssignments) {
                            if (assignment.assignment == null) error("expected assignment")

                            val statement = statementGenerator.callMethod(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                assignment.assignment,
                                getEl
                            ).apply {
                                putValueArgument(0, getAttribute(assignment.attribute.name))
                            }
                            statements.add(statement)
                        }
                        statements.add(getEl)

                        result = IrBlockImpl(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            type = elType.toIrType()!!,
                            origin = IrStatementOrigin.LAMBDA,
                            statements = statements
                        )
                    }

                    // TODO(lmr): this doesn't work for what appears to be a broader compiler bug, but it is the more ideal
                    // way to write this code. The above code does the same thing but we should switch it whenever the bug in
                    // the compiler is fixed.
//                    if (callNode.postAssignments.isNotEmpty()) {
//                        if (callNode.applyCall == null) error("applyCall expected to be non-null")
//                        if (callNode.applyLambdaDescriptor == null) error("applyLambdaDescriptor expected to be non-null")
//                        if (callNode.applyLambdaType == null) error("applyLambdaType expected to be non-null")
//
//                        result = statementGenerator.buildCall(
//                            element.startOffset,
//                            element.endOffset,
//                            resolvedCall = callNode.applyCall,
//                            descriptor = callNode.applyCall.resultingDescriptor as FunctionDescriptor,
//                            receiver = result
//                        ).apply {
//                            val applyLambda = lambdaExpression(callNode.applyLambdaDescriptor, callNode.applyLambdaType) { statements ->
//                                val validator = statementGenerator.extensionReceiverOf(callNode.applyLambdaDescriptor)
//                                    ?: error("Expected extension receiver")
//
//                                for (assignment in callNode.postAssignments) {
//                                    if (assignment.assignment == null) error("expected assignmnet")
//
//                                    val statement = statementGenerator.callMethod(
//                                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
//                                        assignment.assignment,
//                                        validator
//                                    ).apply {
//                                        putValueArgument(0, getAttribute(assignment.attribute.name))
//                                    }
//                                    statements.add(statement)
//                                }
//                                statementGenerator.addReturn(statements, callNode.applyLambdaDescriptor)
//                            }
//                            putValueArgument(0, applyLambda)
//                        }
//                    }

                    val nextCall = callNode.nextCall
                    if (nextCall != null) {
                        result = generateEmitOrCallNode(nextCall, container, result)
                    }

                    return result
                }
                is MemoizedCallNode -> {
                    val memoize = callNode.memoize
                    val composerCall = memoize.composerCall

                    val composerCallParametersType = composerCall.resultingDescriptor.valueParameters.map { it.name to it.type }.toMap()
                    val composerCallParameters = composerCall.resultingDescriptor.valueParameters.map { it.name to it }.toMap()

                    return statementGenerator.callMethod(
                        element.startOffset,
                        element.endOffset,
                        composerCall,
                        getComposer
                    ).apply {
                        // Place the key parameter
                        putValueArgument(composerCallParameters[KtxNameConventions.CALL_KEY_PARAMETER]!!, keyExpression(memoize))

                        // Place the ctor parameter
                        if (memoize.ctorCall != null) {
                            val ctorLambdaDescriptor = memoize.callCtorFnDescriptor!!
                            val ctorParameterType = composerCallParametersType[KtxNameConventions.CALL_CTOR_PARAMETER]!!
                            val ctorLambda = lambdaExpression(ctorLambdaDescriptor, ctorParameterType) { statements ->
                                val ctorCall = statementGenerator.buildCall(
                                    openTagName.startOffset,
                                    openTagName.endOffset,
                                    memoize.ctorCall,
                                    memoize.ctorCall.resultingDescriptor.original as FunctionDescriptor
                                ).apply {
                                    putValueParameters(memoize.ctorParams, statementGenerator) { getAttribute(it) }
                                }
                                // OUTPUT: return <ctor>(<arguments>)
                                val scopeOwner = statementGenerator.context.symbolTable.referenceFunction(ctorLambdaDescriptor)
                                statements.add(
                                    IrReturnImpl(
                                        openTagName.startOffset, openTagName.endOffset,
                                        statementGenerator.context.irBuiltIns.nothingType,
                                        scopeOwner, ctorCall
                                    )
                                )
                            }
                            putValueArgument(composerCallParameters[KtxNameConventions.CALL_CTOR_PARAMETER]!!, ctorLambda)
                        }

                        // Place the validation parameter
                        val validateLambdaDescriptor = memoize.callInvalidFnDescriptor!!
                        val validateParameterType = composerCallParametersType[KtxNameConventions.CALL_INVALID_PARAMETER]!!
                        val validateLambda = lambdaExpression(validateLambdaDescriptor, validateParameterType) { statements ->
                            // all as one expression: a or b or c ... or z

                            val validationCalls = memoize.validations
                                .map { validation ->
                                    statementGenerator.validationCall(
                                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        validation,
                                        validateLambdaDescriptor,
                                        validateLambdaDescriptor.valueParameters.firstOrNull()
                                    ) { name ->
                                        getAttribute(name)
                                    }
                                }
                            when (validationCalls.size) {
                                0 -> Unit // TODO(lmr): return constant true here?
                                1 -> statements.add(validationCalls.single())
                                else -> {
                                    statements.add(
                                        validationCalls.reduce { left, right ->
                                            statementGenerator.callMethod(
                                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                                resolvedKtxCall.infixOrCall,
                                                left
                                            ).apply {
                                                putValueArgument(0, right)
                                            }
                                        }
                                    )
                                }
                            }

                            statementGenerator.addReturn(statements, validateLambdaDescriptor)
                        }
                        putValueArgument(composerCallParameters[KtxNameConventions.CALL_INVALID_PARAMETER]!!, validateLambda)

                        // Place the last argument, which is where the "call" takes place
                        val bodyLambdaDescriptor = memoize.callBlockFnDescriptor!!
                        val bodyParameterType = composerCallParametersType[KtxNameConventions.CALL_BLOCK_PARAMETER]!!
                        val bodyLambda = lambdaExpression(bodyLambdaDescriptor, bodyParameterType) { statements ->
                            val nextReceiver = bodyLambdaDescriptor.valueParameters.firstOrNull()?.let {
                                val receiverValue = statementGenerator.context.symbolTable.referenceValue(it)
                                IrGetValueImpl(
                                    UNDEFINED_OFFSET,
                                    UNDEFINED_OFFSET,
                                    it.type.toIrType()!!,
                                    receiverValue
                                )
                            }
                            statements.add(generateEmitOrCallNode(callNode.call, bodyLambdaDescriptor, nextReceiver))
                        }
                        putValueArgument(composerCallParameters[KtxNameConventions.CALL_BLOCK_PARAMETER]!!, bodyLambda)
                    }
                }
                is ErrorNode -> error("KTX element resolved to error node. Cannot generate.")
            }
        }

        statements.add(generateEmitOrCallNode(resolvedKtxCall.emitOrCall, statementGenerator.scopeOwner))

        if (statements.size == 1) return statements.first()

        return IrBlockImpl(
            element.startOffset,
            element.endOffset,
            irBuiltIns.unitType,
            null,
            statements
        )
    }
}

private fun <T> Collection<T>.append(collection: Collection<T>): Collection<T> {
    if (collection.isEmpty()) return this
    if (this.isEmpty()) return collection
    val result = arrayListOf<T>()
    result.addAll(this)
    result.addAll(collection)
    return result
}

private fun StatementGenerator.getProperty(startOffset: Int, endOffset: Int, property: ResolvedCall<*>): IrCall {
    val functionDescriptor = (property.resultingDescriptor as PropertyDescriptor).getter ?: error("Unexpected type resolution")
    return buildCall(
        startOffset,
        endOffset,
        property,
        functionDescriptor,
        irStatementOrigin = IrStatementOrigin.GET_PROPERTY
    )
}

private fun StatementGenerator.extensionReceiverOf(descriptor: FunctionDescriptor): IrExpression? {
    return descriptor.extensionReceiverParameter?.let {
        val receiverValue = context.symbolTable.referenceValue(it)
        IrGetValueImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            it.type.toIrType(),
            receiverValue
        )
    }
}

private fun StatementGenerator.addReturn(statements: MutableList<IrStatement>, descriptor: FunctionDescriptor) {
    val count = statements.size
    if (count >= 1) {
        // Replace the last statement with a return
        val lastStatement = statements[count - 1]
        if (lastStatement !is IrReturn) {
            val scopeOwner = context.symbolTable.referenceFunction(descriptor)
            statements[count - 1] = IrReturnImpl(
                lastStatement.startOffset,
                lastStatement.endOffset,
                context.irBuiltIns.nothingType,
                scopeOwner,
                lastStatement as IrExpression
            )
        }
    }
}

private fun StatementGenerator.validationCall(
    startOffset: Int,
    endOffset: Int,
    validation: ValidatedAssignment,
    fnDescriptor: FunctionDescriptor,
    assignmentReceiver: ValueDescriptor?,
    getAttribute: (String) -> IrExpression
): IrCall {
    val name = validation.attribute.name
    val attributeValue = getAttribute(name)
    val validator = extensionReceiverOf(fnDescriptor)
        ?: error("expected an extension receiver to validator lambda")

    // for emit, fnDescriptor is Validator.(Value) -> Unit    or Validator.(Value, Element.(Value) -> Unit) -> Unit
    // for call, fnDescriptor is Validator.(Value) -> Boolean or Validator.(Value, (Value) -> Unit) -> Boolean

    // in emit, the element is passed through an extension parameter
    // in call, the element is passed through a capture scope

    return callMethod(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        validation.validationCall!!,
        validator
    ).apply {
        putValueArgument(0, attributeValue)
        val assignment = validation.assignment
        if (assignment != null && validation.validationType != ValidationType.CHANGED) {
            val assignmentLambdaDescriptor = validation.assignmentLambda!!
            val validationAssignment = lambdaExpression(
                startOffset, endOffset,
                assignmentLambdaDescriptor,
                validation.validationCall.resultingDescriptor.valueParameters[1].type
            ) { statements ->
                val parameterDefinition = validation.assignmentLambda.valueParameters.first()
                val parameterReference = context.symbolTable.referenceValueParameter(parameterDefinition)
                val receiver = assignmentReceiver?.let {
                    val receiverValue = context.symbolTable.referenceValue(assignmentReceiver)
                    IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        assignmentReceiver.type.toIrType(),
                        receiverValue
                    )
                } ?: error("Expected an assignment receiver for validations that have assignments")

                val assignmentStatement = callMethod(
                    startOffset,
                    endOffset,
                    assignment,
                    receiver
                ).apply {
                    putValueArgument(
                        0, IrGetValueImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            parameterReference
                        )
                    )
                }
                statements.add(assignmentStatement)
                addReturn(statements, assignmentLambdaDescriptor)
            }
            putValueArgument(1, validationAssignment)
        }
    }
}

private fun IrCall.putValueParameters(
    parameters: List<ValueNode>,
    statementGenerator: StatementGenerator,
    ctorValueParameters: List<ValueParameterDescriptor> = emptyList(),
    getAttribute: (String) -> IrExpression
) {
    parameters.forEachIndexed { index, parameter ->
        when (parameter) {
            is DefaultValueNode -> {
                // do nothing
            }
            is ImplicitCtorValueNode -> {
                val implicitLambdaParameterCandidates = ctorValueParameters.filter {
                    it.type.isSubtypeOf(parameter.type)
                }
                assert(implicitLambdaParameterCandidates.size == 1) {
                    if (implicitLambdaParameterCandidates.isEmpty())
                        "missing implicit constructor parameter ${parameter.name}: ${parameter.type}"
                    else "ambiguous implicit constructor parameters ${parameter.name}: ${parameter.type}"
                }
                val implicitCtorParameter = implicitLambdaParameterCandidates.single()
                val implicitCtorParameterReference =
                    statementGenerator.context.symbolTable.referenceValue(implicitCtorParameter)
                putValueArgument(
                    index,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        implicitCtorParameter.type.toIrType()!!,
                        implicitCtorParameterReference
                    )
                )
            }
            is AttributeNode -> putValueArgument(index, getAttribute(parameter.name))
        }
    }
}

private fun StatementGenerator.callFunction(
    startOffset: Int,
    endOffset: Int,
    function: ResolvedCall<*>,
    extensionReceiver: IrExpression? = null
): IrCall {
    val functionDescriptor = function.resultingDescriptor as FunctionDescriptor

    return buildCall(startOffset, endOffset, function, functionDescriptor, extensionReceiver = extensionReceiver)
}

private fun StatementGenerator.callMethod(
    startOffset: Int,
    endOffset: Int,
    function: ResolvedCall<*>,
    dispatchReceiver: IrExpression
): IrCall {
    val functionDescriptor = function.resultingDescriptor as FunctionDescriptor

    return buildCall(startOffset, endOffset, function, functionDescriptor, dispatchReceiver)
}

private fun StatementGenerator.buildCall(
    startOffset: Int,
    endOffset: Int,
    resolvedCall: ResolvedCall<*>,
    descriptor: FunctionDescriptor,
    receiver: IrExpression? = null,
    dispatchReceiver: IrExpression? = null,
    extensionReceiver: IrExpression? = null,
    irStatementOrigin: IrStatementOrigin? = null
): IrCall {
    val callBuilder = pregenerateCallReceivers(resolvedCall)
    return callBuilder.callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
        val returnType = descriptor.returnType!!
        val functionSymbol = context.symbolTable.referenceFunction(descriptor.original)
        IrCallImpl(
            startOffset, endOffset,
            returnType.toIrType(),
            functionSymbol,
            descriptor,
            resolvedCall.typeArguments.count(),
            irStatementOrigin,
            null
        ).apply {
            when (resolvedCall.explicitReceiverKind) {
                ExplicitReceiverKind.DISPATCH_RECEIVER -> {
                    this.dispatchReceiver = dispatchReceiver ?: receiver ?: dispatchReceiverValue?.load()
                    this.extensionReceiver = extensionReceiver ?: extensionReceiverValue?.load()
                }
                ExplicitReceiverKind.EXTENSION_RECEIVER -> {
                    this.dispatchReceiver = dispatchReceiver ?: dispatchReceiverValue?.load()
                    this.extensionReceiver = extensionReceiver ?: receiver ?: extensionReceiverValue?.load()
                }
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> {
                    this.dispatchReceiver = dispatchReceiver ?: dispatchReceiverValue?.load()
                    this.extensionReceiver = extensionReceiver ?: extensionReceiverValue?.load()
                }
                ExplicitReceiverKind.BOTH_RECEIVERS -> {
                    TODO("Figure out how to handle both receivers")
                }
            }
            putTypeArguments(resolvedCall.typeArguments) { it.toIrType() }
        }
    } as IrCall
}

private fun getKeyValue(descriptor: DeclarationDescriptor, startOffset: Int): Int = descriptor.fqNameSafe.toString().hashCode() xor startOffset

private fun buildLambda(
    context: GeneratorContext,
    startOffset: Int,
    endOffset: Int,
    descriptor: FunctionDescriptor,
    body: (statements: MutableList<IrStatement>) -> Unit
): IrSimpleFunction {
    return context.symbolTable.declareSimpleFunctionWithOverrides(
        startOffset = startOffset,
        endOffset = endOffset,
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
        descriptor = descriptor
    )
        .buildWithScope(context) { function ->
            fun declareParameter(descriptor: ParameterDescriptor) =
                context.symbolTable.declareValueParameter(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = IrDeclarationOrigin.DEFINED,
                    descriptor = descriptor,
                    type = descriptor.type.toIrType(context.symbolTable)!!
                )


            function.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { declareParameter(it) }
            function.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { declareParameter(it) }
            descriptor.valueParameters.mapTo(function.valueParameters) { declareParameter(it) }

            val statements = mutableListOf<IrStatement>()
            body(statements)
            function.body = IrBlockBodyImpl(startOffset, endOffset, statements)
            function.returnType = descriptor.returnType?.toIrType(context.symbolTable) ?: error("unable to find return type")
        }
}

private fun StatementGenerator.lambdaExpression(
    startOffset: Int,
    endOffset: Int,
    descriptor: FunctionDescriptor,
    kotlinType: KotlinType,
    block: (statements: MutableList<IrStatement>) -> Unit
): IrExpression {
    val declaration = buildLambda(context, startOffset, endOffset, descriptor, block)
    val type = kotlinType.toIrType()
    return IrBlockImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        origin = IrStatementOrigin.LAMBDA,
        statements = mutableListOf(
            declaration,
            IrFunctionReferenceImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = type,
                symbol = declaration.symbol,
                descriptor = declaration.symbol.descriptor,
                typeArgumentsCount = 0,
                origin = IrStatementOrigin.LAMBDA
            )
        )
    )
}