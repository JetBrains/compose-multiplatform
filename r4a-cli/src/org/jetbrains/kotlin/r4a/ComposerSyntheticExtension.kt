package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class ComposerSyntheticExtension : SyntheticIrExtension {
    override fun visitKtxElement(statementGenerator: StatementGenerator, element: KtxElement): IrStatement {
        val resolvedKtxCall =
            statementGenerator.context.bindingContext.get(R4AWritableSlices.RESOLVED_KTX_CALL, element) ?: error("no resolution info")
        val openTagName = element.simpleTagName ?: element.qualifiedTagName ?: error("malformed element")
        val resolvedAttributes = resolvedKtxCall.usedAttributes.map { it.name to it }.toMap()
        val tagAttributes = element.attributes.map { it.name to it }.toMap()

        val statements = mutableListOf<IrStatement>()

        // Convert tag attribute expressions
        val attributeExpressions = element.attributes.map { attribute ->
            val valueExpression = attribute.value ?: attribute.key ?: error("malformed element PSI")
            val irValueExpression = KtPsiUtil.safeDeparenthesize(valueExpression).accept(statementGenerator, null) as? IrExpression
                    ?: error("attributes need to be expressions")

            if (irValueExpression is IrConst<*>) {
                // For constant expression don't create the temporary.
                attribute.name to irValueExpression
            } else {
                // Create a temporary variable to hold the value of the expression
                val resolvedAttribute =
                    resolvedAttributes[attribute.name] ?: error("Unexpected missing resolved attribute ${attribute.name}")
                val attributeType = resolvedAttribute.type
                val attributeVariable = IrTemporaryVariableDescriptorImpl(
                    statementGenerator.scopeOwner,
                    Name.identifier("__el_attr_${attribute.name}"),
                    attributeType,
                    false
                )

                val attrVariableDeclaration = statementGenerator.context.symbolTable.declareVariable(
                    attribute.startOffset, attribute.endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    attributeVariable,
                    attributeType.toIrType()!!,
                    irValueExpression
                )
                // OUTPUT: var _el_attr_name = <attribute expression>
                statements.add(attrVariableDeclaration)

                val getValue = IrGetValueImpl(
                    attribute.startOffset, attribute.endOffset,
                    statementGenerator.context.symbolTable.referenceVariable(attributeVariable)
                )

                attribute.name to getValue
            }
        }.toMap()


        val getComposer = statementGenerator.getProperty(
            openTagName.startOffset, openTagName.endOffset,
            resolvedKtxCall.getComposerCall
        )

        val bodyStatements = element.body
        val lambda = element.bodyLambdaExpression

        if (bodyStatements != null || lambda != null) {
            error("Nested tags not implemented yet")
        }

        val unitType = with(statementGenerator) { context.builtIns.unitType.toIrType() }

        fun keyExpression(callInfo: ComposerCallInfo): IrExpression {
            val sourceKey = getKeyValue(statementGenerator.scopeOwner, element.startOffset)
            return listOf<IrExpression>(
                IrConstImpl.int(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    statementGenerator.context.builtIns.intType.toIrType()!!,
                    sourceKey
                )
            ).append(callInfo.pivotals.map {
                attributeExpressions[it.name] ?: error("Missing pivotal argument")
            }).reduce { left, right ->
                statementGenerator.callFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, callInfo.joinKeyCall ?: error("Missing joinKeyCall"))
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
        ): IrExpression {
            val declaration = buildLambda(statementGenerator.context, openTagName.startOffset, openTagName.endOffset, descriptor, block)
            val type = kotlinType.toIrType()!!
            return IrBlockImpl(
                startOffset = openTagName.startOffset,
                endOffset = openTagName.endOffset,
                type = type,
                origin = IrStatementOrigin.LAMBDA,
                statements = mutableListOf(
                    declaration,
                    IrFunctionReferenceImpl(
                        startOffset = openTagName.startOffset,
                        endOffset = openTagName.endOffset,
                        type = type,
                        symbol = declaration.symbol,
                        descriptor = declaration.symbol.descriptor,
                        typeArgumentsCount = 0,
                        origin = IrStatementOrigin.LAMBDA
                    )
                )
            )
        }

        val call = resolvedKtxCall.emitOrCall
        when (call) {
            is EmitCallNode -> {
                val memoize = call.memoize
                val composerCall = memoize.composerCall ?: error("Unexpected type resolution")

                val composerCallParametersType = composerCall.resultingDescriptor.valueParameters.map { it.type }

                statements.add(statementGenerator.callMethod(
                    element.startOffset,
                    element.endOffset,
                    composerCall,
                    getComposer
                ).apply {
                    // TODO(b/???): Theses should be based on name, not index.

                    // Place the key parameter
                    putValueArgument(0, keyExpression(memoize))

                    // Place the ctor parameter
                    val ctor = memoize.ctorCall!!
                    val parameters = memoize.ctorParams
                    val ctorLambdaDescriptor = memoize.functionDescriptors[1]!!
                    val ctorValueParameters = ctorLambdaDescriptor.valueParameters
                    val ctorLambda = lambdaExpression(ctorLambdaDescriptor, composerCallParametersType[1]) { statements ->
                        val ctorCall = statementGenerator.buildCall(
                            openTagName.startOffset,
                            openTagName.endOffset,
                            ctor,
                            ctor.resultingDescriptor.original as FunctionDescriptor
                        ).apply {
                            parameters.forEachIndexed { index, parameter ->
                                when (parameter) {
                                    is ImplicitCtorValueNode -> {
                                        val implicitLambdaParameterCandidates = ctorValueParameters.filter {
                                            it.type.isSubtypeOf(parameter.type)
                                        }
                                        assert(implicitLambdaParameterCandidates.size == 1) {
                                            if (implicitLambdaParameterCandidates.isEmpty())
                                                "missing implicit constructor parameter"
                                            else "ambiguous implicit constructor parameters"
                                        }
                                        val implicitCtorParameter = implicitLambdaParameterCandidates.first()
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
                                    is AttributeNode -> {
                                        putValueArgument(
                                            index,
                                            attributeExpressions[parameter.name] ?: error("Missing attribute ${parameter.name}")
                                        )
                                    }
                                }
                            }
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
                    putValueArgument(1, ctorLambda)

                    fun extensionReceiverOf(descriptor: FunctionDescriptor): IrExpression? {
                        return descriptor.extensionReceiverParameter?.let {
                            val receiverValue = statementGenerator.context.symbolTable.referenceValue(it)
                            IrGetValueImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                it.type.toIrType()!!,
                                receiverValue
                            )
                        }
                    }

                    fun addReturn(statements: MutableList<IrStatement>, descriptor: FunctionDescriptor) {
                        val count = statements.size
                        if (count >= 1) {
                            // Replace the last statement with a return
                            val lastStatement = statements[count - 1]
                            if (lastStatement !is IrReturn) {
                                val scopeOwner = statementGenerator.context.symbolTable.referenceFunction(descriptor)
                                statements[count - 1] = IrReturnImpl(
                                    lastStatement.startOffset,
                                    lastStatement.endOffset,
                                    statementGenerator.context.irBuiltIns.nothingType,
                                    scopeOwner,
                                    lastStatement as IrExpression
                                )
                            }
                        }
                    }

                    // Place the validation parameter
                    val validateLambdaDescriptor = memoize.functionDescriptors[2]!!
                    val validateLambda = lambdaExpression(validateLambdaDescriptor, composerCallParametersType[2]) { statements ->
                        for (validation in memoize.validations) {
                            val name = validation.attribute.name
                            val tagAttribute = tagAttributes[name] ?: error("could not find corresponding attribute $name")
                            val attributeValue = attributeExpressions[name] ?: error("could not find corresponding expression for $name")
                            val startOffset = tagAttribute.startOffset
                            val endOffset = tagAttribute.endOffset
                            val validator = extensionReceiverOf(validateLambdaDescriptor)
                                    ?: error("expected an extension receiver to validator lambda")
                            statements.add(statementGenerator.callMethod(
                                startOffset,
                                endOffset,
                                validation.validationCall!!,
                                validator
                            ).apply {
                                putValueArgument(0, attributeValue)
                                val assignment = validation.assignment
                                if (assignment != null) {
                                    val validationLambdaDescriptor = validation.assignmentLambda!!
                                    val validationAssignment = lambdaExpression(
                                        validationLambdaDescriptor,
                                        validation.validationCall.resultingDescriptor.valueParameters[1].type
                                    ) { statements ->
                                        val parameterDefinition = validation.assignmentLambda.valueParameters.first()
                                        val parameterReference =
                                            statementGenerator.context.symbolTable.referenceValueParameter(parameterDefinition)
                                        val emittedElement = extensionReceiverOf(validationLambdaDescriptor)
                                                ?: error("expected a dispatch receiver on update lambda")
                                        val assignmentStatement = statementGenerator.callMethod(
                                            startOffset,
                                            endOffset,
                                            assignment,
                                            emittedElement
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
                                        addReturn(statements, validationLambdaDescriptor)
                                    }
                                    putValueArgument(1, validationAssignment)
                                }
                            })
                        }

                        addReturn(statements, validateLambdaDescriptor)
                    }
                    putValueArgument(2, validateLambda)
                })
            }
        }

        if (statements.size == 1) return statements.first()

        return IrBlockImpl(
            element.startOffset,
            element.endOffset,
            unitType,
            null,
            statements
        )
    }
}

fun <T> Collection<T>.append(collection: Collection<T>): Collection<T> {
    if (collection.isEmpty()) return this
    if (this.isEmpty()) return collection
    val result = arrayListOf<T>()
    result.addAll(this)
    result.addAll(collection)
    return result
}

fun StatementGenerator.getProperty(startOffset: Int, endOffset: Int, property: ResolvedCall<*>): IrCall {
    val functionDescriptor = (property.resultingDescriptor as PropertyDescriptor).getter ?: error("Unexpected type resolution")
    return buildCall(
        startOffset,
        endOffset,
        property,
        functionDescriptor,
        irStatementOrigin = IrStatementOrigin.GET_PROPERTY
    )
}

fun StatementGenerator.callFunction(
    startOffset: Int,
    endOffset: Int,
    function: ResolvedCall<*>,
    extensionReceiver: IrExpression? = null
): IrCall {
    val functionDescriptor = function.resultingDescriptor as FunctionDescriptor

    return buildCall(startOffset, endOffset, function, functionDescriptor, extensionReceiver = extensionReceiver)
}

fun StatementGenerator.callMethod(startOffset: Int, endOffset: Int, function: ResolvedCall<*>, dispatchReceiver: IrExpression): IrCall {
    val functionDescriptor = function.resultingDescriptor as FunctionDescriptor

    return buildCall(startOffset, endOffset, function, functionDescriptor, dispatchReceiver)
}

fun StatementGenerator.buildCall(
    startOffset: Int,
    endOffset: Int,
    resolvedCall: ResolvedCall<*>,
    descriptor: FunctionDescriptor,
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
            this.dispatchReceiver = dispatchReceiver ?: dispatchReceiverValue?.load()
            this.extensionReceiver = extensionReceiver ?: extensionReceiverValue?.load()

            putTypeArguments(resolvedCall.typeArguments) { it.toIrType() }
        }
    } as IrCall
}

fun getKeyValue(descriptor: DeclarationDescriptor, startOffset: Int): Int = descriptor.fqNameSafe.toString().hashCode() xor startOffset

fun buildLambda(
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