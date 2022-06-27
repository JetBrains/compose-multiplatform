/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.codegen.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.resolve.BindingTrace

/**
 * Example:
 *
 * Given Row is inline composable function:
 * ```
 * @Composable
 * inline fun Row(
 *      modifier: Modifier = Modifier,
 *      horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
 *      verticalAlignment: Alignment.Vertical = Alignment.Top,
 *      content: @Composable RowScope.() -> Unit
 * ) {...}
 * ```
 *
 * and given a function calling Row:
 * @Composable
 * fun Foo(content: @Composable RowScope.() -> Unit) {
 *      Row(Modifier, content = content)
 * }
 * the call will be transformed to:
 * ``` Row(Modifier, content = { content() }) // argument 'content' is replaced by wrapping lambda
 *
 * Wrapping lambda will be inlined anyway, so it shouldn't be a problem.
 *
 * This transformation enables `FixComposableLambdaCalls` to transform `content()` call,
 * which otherwise won't be seen by FixComposableLambdaCalls ,
 * because it's not inlined until later lowerings made by kotlin.
 */
class WrapNotInlineableComposableLambdasForJs(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics
) : AbstractComposeLowering(context, symbolRemapper, metrics) {

    private val composableImplementationAnnotation by lazy {
        getTopLevelClass(ComposeFqNames.Composable).owner
    }

    private val currentParent: MutableList<IrSimpleFunction?> = mutableListOf()

    override fun lower(module: IrModuleFragment) {
        require(context.platform.isJs()) {
            "This transformation is intended only for kotlin/js targets"
        }
        module.transformChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        currentParent.push(declaration)
        return super.visitSimpleFunction(declaration).also {
            currentParent.pop()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as IrCall
        val callee = call.symbol.owner

        // we check isDecoy as well, because decoys from klibs don't have Composable annotation
        if (callee.isInline && (callee.hasComposableAnnotation() || callee.isDecoy())) {
            return wrapComposableLambdasInCall(expression)
        }

        return call
    }

    private fun wrapComposableLambdasInCall(call: IrCall): IrCall {
        val owner = call.symbol.owner
        val args = call.valueArgumentsCount

        val mappedArguments = generateSequence(0) { it + 1 }.take(args).map {
            val expr = call.getValueArgument(it)

            return@map when {
                // order of conditions matters
                expr == null -> expr
                !expr.type.isFunction() -> expr
                !expr.type.annotations.hasAnnotation(ComposeFqNames.Composable) -> expr
                expr is IrCall -> wrapLambdaArgument(expr)
                expr !is IrGetValue -> expr
                expr.symbol is IrValueParameterSymbol -> {
                    val valueParameter = expr.symbol.owner as IrValueParameter
                    val cantBeInlined = valueParameter.isNoinline
                        || (valueParameter.parent as? IrSimpleFunction) == null
                        || (valueParameter.parent as? IrSimpleFunction)?.isInline == false
                    val sameParamIsNoInline = owner.valueParameters[it].isNoinline

                    if (cantBeInlined && !sameParamIsNoInline) {
                        wrapLambdaArgument(expr)
                    } else {
                        expr
                    }
                }
                else -> wrapLambdaArgument(expr)
            }
        }.toList()

        // Same call as original but with wrapped (if needed) value arguments
        return IrCallImpl(
            startOffset = call.startOffset, endOffset = call.endOffset,
            type = call.type,
            symbol = call.symbol,
            typeArgumentsCount = call.typeArgumentsCount,
            valueArgumentsCount = call.valueArgumentsCount,
            origin = call.origin
        ).apply {
            dispatchReceiver = call.dispatchReceiver
            extensionReceiver = call.extensionReceiver

            repeat(call.typeArgumentsCount) {
                putTypeArgument(it, call.getTypeArgument(it))
            }
            mappedArguments.forEachIndexed { index, irExpression ->
                putValueArgument(index, irExpression)
            }
        }
    }

    private fun wrapLambdaArgument(
        argument: IrExpression
    ): IrExpression {
        val lambdaType = argument.type as IrSimpleType

        val funExpr = IrFunctionExpressionImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = argument.type,
            origin = IrStatementOrigin.LAMBDA,
            function = context.irFactory.buildFun {
                name = Name.special("<anonymous>")
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                visibility = DescriptorVisibilities.LOCAL
                returnType = lambdaType.arguments.last().typeOrNull!!
            }
        )

        funExpr.function.apply {
            parent = currentParent.peek()!!

            if (lambdaType.isExtensionFunctionType) {
                val extType = lambdaType.arguments.first().typeOrNull!!
                extensionReceiverParameter = buildValueParameter(
                    type = extType,
                    name = Name.identifier("\$extensionReceiver_${extType.classFqName}")
                ).also {
                    it.parent = funExpr.function
                }
            }

            annotations = listOf(
                IrConstructorCallImpl.fromSymbolOwner(
                    type = composableImplementationAnnotation.defaultType,
                    constructorSymbol = composableImplementationAnnotation.constructors.first().symbol
                )
            )

            var ix = if (lambdaType.isExtensionFunctionType) 1 else 0

            valueParameters = lambdaType.arguments.subList(
                ix, lambdaType.arguments.lastIndex
            ).map { argumentType ->
                ix++
                buildValueParameter(
                    type = argumentType.typeOrNull!!,
                    name = Name.identifier("\$wrappingLambdaParam$ix"),
                    ix = ix - 1
                ).also {
                    it.parent = funExpr.function
                }
            }

            body = DeclarationIrBuilder(context, symbol).irBlockBody {
                val invoke = createInvokeForComposableLambda(
                    lambdaType = lambdaType,
                    dispatchReceiver = argument,
                    extensionReceiverParameterSymbol = extensionReceiverParameter?.symbol,
                    valueParameters = valueParameters,
                    arity = valueParameters.size + if (lambdaType.isExtensionFunctionType) 1 else 0
                )
                if (funExpr.function.returnType != context.irBuiltIns.unitType) {
                    +irReturn(symbol, invoke)
                } else {
                    +invoke
                }
            }
        }

        return funExpr
    }

    private fun createInvokeForComposableLambda(
        lambdaType: IrSimpleType,
        dispatchReceiver: IrExpression,
        extensionReceiverParameterSymbol: IrValueParameterSymbol?,
        valueParameters: List<IrValueParameter>,
        arity: Int
    ): IrCall {

        return IrCallImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = lambdaType.arguments.last().typeOrNull!!,
            symbol = (lambdaType.classifier.owner as IrClass).declarations.first { declaration ->
                (declaration as? IrFunction)?.name?.asString() == "invoke"
            }.symbol as IrSimpleFunctionSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = arity
        ).apply {
            this.dispatchReceiver = dispatchReceiver

            var pix = 0
            if (extensionReceiverParameterSymbol != null) {
                putValueArgument(
                    pix, IrGetValueImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        extensionReceiverParameterSymbol
                    )
                )
                pix++
            }
            valueParameters.forEach { valueParamImpl ->
                putValueArgument(
                    pix, IrGetValueImpl(
                        startOffset = SYNTHETIC_OFFSET,
                        endOffset = SYNTHETIC_OFFSET,
                        valueParamImpl.symbol
                    )
                )
                pix++
            }
        }
    }

    private fun buildValueParameter(type: IrType, name: Name, ix: Int = 0): IrValueParameter {
        return IrValueParameterImpl(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            symbol = IrValueParameterSymbolImpl(),
            name = name,
            index = ix,
            type = type,
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
            isAssignable = false
        )
    }
}